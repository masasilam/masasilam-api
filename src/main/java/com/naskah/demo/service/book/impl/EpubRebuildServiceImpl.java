package com.naskah.demo.service.book.impl;

import com.naskah.demo.mapper.BookChapterMapper;
import com.naskah.demo.mapper.BookMapper;
import com.naskah.demo.mapper.ContributorMapper;
import com.naskah.demo.mapper.CorrectionMapper;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.model.entity.BookChapter;
import com.naskah.demo.model.entity.Contributor;
import com.naskah.demo.service.book.EpubRebuildService;
import com.naskah.demo.util.file.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpubRebuildServiceImpl implements EpubRebuildService {

    private final BookMapper        bookMapper;
    private final BookChapterMapper chapterMapper;
    private final CorrectionMapper  correctionMapper;
    private final ContributorMapper contributorMapper;
    private final FileUtil          fileUtil;

    // ── PUBLIC: ASYNC REBUILD ────────────────────────────────

    @Override
    @Async("rebuildExecutor")
    public void rebuildAsync(Long bookId) {
        log.info("[EpubRebuild] Starting async rebuild for book {}", bookId);
        long startTime = System.currentTimeMillis();

        try {
            Book book = bookMapper.findById(bookId);
            if (book == null) {
                log.error("[EpubRebuild] Book {} not found, aborting", bookId);
                return;
            }

            List<BookChapter> chapters = chapterMapper.findChaptersByBookId(bookId);
            if (chapters.isEmpty()) {
                log.warn("[EpubRebuild] Book {} has no chapters, skipping", bookId);
                return;
            }

            log.info("[EpubRebuild] Building epub from {} chapters for book '{}'",
                    chapters.size(), book.getTitle());

            // ── Ambil metadata existing dari epub Cloudinary ──────────────
            // Tujuan: preserve semua metadata kaya (schema:*, collection,
            // alternate-script, rights panjang, dll) yang sudah ada di OPF asli.
            // Kita hanya akan update dcterms:modified dan menambahkan editor baru.
            String existingMetadataXml = null;
            if (book.getFileUrl() != null && !book.getFileUrl().isBlank()) {
                try {
                    byte[] existingEpub = fileUtil.downloadFromUrl(book.getFileUrl());
                    existingMetadataXml = extractMetadataXmlFromEpub(existingEpub);
                    if (existingMetadataXml != null) {
                        log.info("[EpubRebuild] Existing metadata loaded from Cloudinary epub");
                    }
                } catch (Exception e) {
                    log.warn("[EpubRebuild] Could not load existing epub for metadata " +
                            "(will build from scratch): {}", e.getMessage());
                }
            }

            List<String> editors = correctionMapper.findApprovedEditorsByBookId(bookId);
            log.info("[EpubRebuild] editors={}", editors.size());

            // 1. Build epub bytes via epublib
            //    orderedSlugs diisi sesuai urutan allSections (= urutan nav/TOC)
            List<String> orderedSlugs = new ArrayList<>();
            byte[] epubBytes = buildEpubFromChapters(book, chapters, editors, orderedSlugs);
            log.info("[EpubRebuild] Raw epub generated: {} bytes", epubBytes.length);

            // 2. Post-process ZIP:
            //    - Manifest + spine dibangun dari scratch (urutan spine = orderedSlugs)
            //    - Metadata: gunakan existing jika ada, hanya update modified + editor baru
            epubBytes = postProcessEpubZip(epubBytes, editors, book, existingMetadataXml, orderedSlugs);
            log.info("[EpubRebuild] Post-process done: {} bytes", epubBytes.length);

            // 3. Simpan editor ke DB
            saveEditorsToDatabase(bookId, editors);

            // 4. Upload ke Cloudinary (overwrite)
            String newFileUrl = fileUtil.uploadEpubOverwrite(epubBytes, book.getSlug());

            // 5. Update book record
            book.setFileUrl(newFileUrl);
            book.setEpubGeneratedAt(LocalDateTime.now());
            bookMapper.updateBook(book);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[EpubRebuild] Done for '{}' in {}ms -> {}", book.getTitle(), elapsed, newFileUrl);

        } catch (Exception e) {
            log.error("[EpubRebuild] Failed for book {}: {}", bookId, e.getMessage(), e);
        }
    }

    // ── INNER RECORD ──────────────────────────────────────────

    /**
     * Satu section = satu file .xhtml.
     * Satu BookChapter bisa menghasilkan beberapa XhtmlSection jika
     * htmlContent mengandung lebih dari satu h1 (format lama Sigil).
     */
    private record XhtmlSection(
            String slug,
            String title,
            String htmlContent,
            String chapterEpubType  // null jika chapter biasa
    ) {}

    // ── PRIVATE: EKSTRAK METADATA DARI EPUB EXISTING ─────────

    /**
     * Baca ZIP epub, temukan OPF, ekstrak blok metadata sebagai string mentah.
     * Hasilnya dipakai sebagai base metadata di OPF baru sehingga semua field
     * kaya (schema:*, collection, alternate-script, dll) tetap utuh.
     *
     * @return string isi {@code <metadata>...</metadata>} (termasuk tag
     *         pembuka/penutup), atau {@code null} jika tidak ditemukan.
     */
    private String extractMetadataXmlFromEpub(byte[] epubBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(epubBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".opf")) {
                    String opfContent = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    Matcher m = Pattern.compile(
                            "(<metadata\\b[^>]*>.*?</metadata>)",
                            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
                    ).matcher(opfContent);
                    if (m.find()) {
                        return m.group(1);
                    }
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            log.warn("[EpubRebuild] extractMetadataXmlFromEpub error: {}", e.getMessage());
        }
        return null;
    }

    // ── PRIVATE: BUILD EPUB ──────────────────────────────────

    /**
     * @param orderedSlugs list kosong yang akan diisi dengan slug section
     *                     sesuai urutan allSections = urutan nav/TOC.
     */
    private byte[] buildEpubFromChapters(Book book,
                                         List<BookChapter> chapters,
                                         List<String> editors,
                                         List<String> orderedSlugs) throws IOException {

        nl.siegmann.epublib.domain.Book epubBook = new nl.siegmann.epublib.domain.Book();
        epubBook.getMetadata().addTitle(book.getTitle());
        if (book.getDescription() != null) {
            epubBook.getMetadata().addDescription(book.getDescription());
        }

        // CSS
        try (var cssStream = getClass().getResourceAsStream("/epub/masasilam.css")) {
            if (cssStream != null) {
                epubBook.getResources().add(new Resource(cssStream, "Styles/masasilam.css"));
            } else {
                log.warn("[EpubRebuild] masasilam.css not found in classpath");
            }
        }

        // Cover
        if (book.getCoverImageUrl() != null) {
            try {
                byte[] coverBytes = fileUtil.downloadFromUrl(book.getCoverImageUrl());
                String coverExt   = detectExtension(book.getCoverImageUrl(), "png");
                String coverPath  = "Images/cover." + coverExt;
                epubBook.setCoverImage(new Resource(coverBytes, coverPath));
            } catch (Exception e) {
                log.warn("[EpubRebuild] Could not embed cover: {}", e.getMessage());
            }
        }

        Map<String, String> imageUrlToLocalPath = new HashMap<>();

        // Split setiap chapter menjadi sections per-h1
        List<XhtmlSection> allSections = new ArrayList<>();
        for (BookChapter chapter : chapters) {
            String processedHtml = embedImagesInHtml(
                    chapter.getHtmlContent(), epubBook, imageUrlToLocalPath);
            allSections.addAll(splitChapterByH1(chapter, processedHtml));
        }
        log.info("[EpubRebuild] Total xhtml sections after h1-split: {}", allSections.size());

        // Nav document
        String navXhtml = buildNavXhtml(allSections);
        epubBook.getResources().add(
                new Resource(navXhtml.getBytes(StandardCharsets.UTF_8), "Text/nav.xhtml"));

        // Tambahkan setiap section ke epub DAN catat slug sesuai urutan
        for (XhtmlSection section : allSections) {
            orderedSlugs.add(section.slug()); // catat urutan untuk spine OPF
            String xhtml        = wrapSectionAsXhtml(section, editors);
            String resourcePath = "Text/" + section.slug() + ".xhtml";
            epubBook.addSection(section.title(),
                    new Resource(xhtml.getBytes(StandardCharsets.UTF_8), resourcePath));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new EpubWriter().write(epubBook, baos);
        return baos.toByteArray();
    }

    // ── PRIVATE: SPLIT CHAPTER BY H1 ─────────────────────────

    private List<XhtmlSection> splitChapterByH1(BookChapter chapter, String html) {
        if (html == null || html.isBlank()) {
            return List.of(new XhtmlSection(chapter.getSlug(), chapter.getTitle(), "<p></p>", null));
        }

        // Deteksi epub:type (halaman judul, kolofon, dll) → tidak dipecah
        String epubType = null;
        Matcher etm = Pattern.compile("epub:type=[\"']([^\"']+)[\"']").matcher(html);
        if (etm.find()) epubType = etm.group(1);

        if (epubType != null) {
            return List.of(new XhtmlSection(chapter.getSlug(), chapter.getTitle(), html, epubType));
        }

        // Cari semua <h1>
        Pattern h1Pattern = Pattern.compile("(?i)(<h1(?:[^>]*)>)(.*?)(</h1>)", Pattern.DOTALL);
        Matcher m = h1Pattern.matcher(html);
        List<int[]> h1Positions = new ArrayList<>();
        while (m.find()) {
            h1Positions.add(new int[]{m.start(), m.end(), m.start(2), m.end(2)});
        }

        if (h1Positions.isEmpty()) {
            return List.of(new XhtmlSection(chapter.getSlug(), chapter.getTitle(), html, null));
        }

        if (h1Positions.size() == 1) {
            int[] pos     = h1Positions.get(0);
            String h1Text = stripTags(html.substring(pos[2], pos[3])).trim();
            String title  = !h1Text.isBlank() ? h1Text : chapter.getTitle();
            return List.of(new XhtmlSection(chapter.getSlug(), title, html, null));
        }

        // Multiple h1 → pecah per-h1
        log.info("[EpubRebuild] Chapter '{}' has {} h1 headings — splitting",
                chapter.getSlug(), h1Positions.size());

        List<XhtmlSection> sections = new ArrayList<>();
        for (int i = 0; i < h1Positions.size(); i++) {
            int[] pos      = h1Positions.get(i);
            int contentEnd = (i + 1 < h1Positions.size())
                    ? h1Positions.get(i + 1)[0] : html.length();
            String h1Text  = stripTags(html.substring(pos[2], pos[3])).trim();
            String title   = !h1Text.isBlank() ? h1Text : chapter.getTitle() + " (" + (i + 1) + ")";
            String content = html.substring(pos[0], contentEnd);
            String slug    = (i == 0) ? chapter.getSlug() : chapter.getSlug() + "-s" + (i + 1);
            sections.add(new XhtmlSection(slug, title, content, null));
        }
        return sections;
    }

    // ── PRIVATE: NAV DOCUMENT ────────────────────────────────

    private String buildNavXhtml(List<XhtmlSection> sections) {
        StringBuilder tocItems      = new StringBuilder();
        StringBuilder landmarkItems = new StringBuilder();
        boolean bodyStartSet        = false;
        boolean inBab               = false;
        StringBuilder pasalBuffer   = new StringBuilder();

        for (XhtmlSection section : sections) {
            String href  = section.slug() + ".xhtml";
            String title = escapeXml(section.title());
            String slug  = section.slug();
            boolean isBab   = (slug.matches("bab-[ivxIVX\\d]+-.*") || slug.startsWith("bab-"))
                    && !slug.matches(".*-s\\d+$");
            boolean isPasal = slug.startsWith("pasal-") && !slug.matches(".*-s\\d+$");
            boolean isSplit = slug.matches(".*-s\\d+$");

            if (isBab) {
                if (inBab) {
                    flushPasalBuffer(tocItems, pasalBuffer);
                    tocItems.append("      </li>\n");
                    pasalBuffer = new StringBuilder();
                }
                tocItems.append("      <li><a href=\"").append(href).append("\">")
                        .append(title).append("</a>\n");
                inBab = true;

            } else if ((isPasal || isSplit) && inBab) {
                pasalBuffer.append("          <li><a href=\"").append(href).append("\">")
                        .append(title).append("</a></li>\n");

            } else {
                if (inBab) {
                    flushPasalBuffer(tocItems, pasalBuffer);
                    tocItems.append("      </li>\n");
                    pasalBuffer = new StringBuilder();
                    inBab = false;
                }
                tocItems.append("      <li><a href=\"").append(href).append("\">")
                        .append(title).append("</a></li>\n");
            }

            String epubType     = section.chapterEpubType() != null ? section.chapterEpubType() : "";
            boolean isTitlePage = epubType.contains("titlepage");
            boolean isColophon  = epubType.contains("colophon");
            boolean isFront     = slug.equals("halaman-judul") || slug.equals("kolofon")
                    || slug.equals("uncopyright");

            if (isTitlePage) {
                landmarkItems.append("      <li><a epub:type=\"cover\" href=\"")
                        .append(href).append("\">Sampul</a></li>\n");
            } else if (isColophon) {
                landmarkItems.append("      <li><a epub:type=\"frontmatter\" href=\"")
                        .append(href).append("\">Kolofon</a></li>\n");
            } else if (!bodyStartSet && !isFront && !isTitlePage && !isColophon) {
                landmarkItems.append("      <li><a epub:type=\"bodymatter\" href=\"")
                        .append(href).append("\">Mulai Membaca</a></li>\n");
                bodyStartSet = true;
            }
        }

        if (inBab) {
            flushPasalBuffer(tocItems, pasalBuffer);
            tocItems.append("      </li>\n");
        }
        landmarkItems.append(
                "      <li><a epub:type=\"toc\" href=\"nav.xhtml\">Daftar Isi</a></li>\n");

        return """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml"
                      xmlns:epub="http://www.idpf.org/2007/ops"
                      xml:lang="id" lang="id">
                <head>
                  <meta name="language" content="Indonesian"/>
                  <meta name="dc:language" content="id"/>
                  <title>DAFTAR ISI</title>
                  <link rel="stylesheet" type="text/css" href="../Styles/masasilam.css"/>
                </head>
                <body epub:type="frontmatter">
                  <nav epub:type="toc" id="toc" role="doc-toc">
                    <h1>DAFTAR ISI</h1>
                    <ol>
                %s    </ol>
                  </nav>
                  <nav epub:type="landmarks" id="landmarks" hidden="">
                    <h1>Landmarks</h1>
                    <ol>
                %s    </ol>
                  </nav>
                </body>
                </html>
                """.formatted(tocItems, landmarkItems);
    }

    private void flushPasalBuffer(StringBuilder toc, StringBuilder buf) {
        if (buf.length() > 0) {
            toc.append("        <ol>\n").append(buf).append("        </ol>\n");
        }
    }

    // ── PRIVATE: WRAP SECTION AS XHTML ───────────────────────

    private String wrapSectionAsXhtml(XhtmlSection section, List<String> editors) {
        String safeTitle     = escapeXml(section.title());
        String sanitizedHtml = sanitizeForXhtml(section.htmlContent());
        String epubType      = section.chapterEpubType();
        boolean hasEpubType  = epubType != null && !epubType.isBlank();
        boolean isColophon   = hasEpubType && epubType.contains("colophon");

        if (isColophon && editors != null && !editors.isEmpty()) {
            sanitizedHtml = injectEditorIntoColophon(sanitizedHtml, editors);
        }

        String bodyAttrs;
        String bodyContent;

        if (hasEpubType) {
            bodyContent = sanitizedHtml;
            if (epubType.contains("titlepage")) {
                bodyAttrs = " epub:type=\"frontmatter\"";
            } else if (isColophon || epubType.contains("imprint")
                    || epubType.contains("copyright-page")) {
                bodyAttrs = " epub:type=\"backmatter\"";
            } else {
                bodyAttrs = "";
            }
        } else {
            bodyContent = "<section class=\"chapter\" epub:type=\"chapter\">\n    " +
                    sanitizedHtml + "\n</section>";
            bodyAttrs = "";
        }

        return """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml"
                      xmlns:epub="http://www.idpf.org/2007/ops"
                      xml:lang="id" lang="id">
                <head>
                    <meta name="language" content="Indonesian"/>
                    <meta name="dc:language" content="id"/>
                    <title>%s</title>
                    <link rel="stylesheet" type="text/css" href="../Styles/masasilam.css"/>
                </head>
                <body%s>
                    %s
                </body>
                </html>
                """.formatted(safeTitle, bodyAttrs, bodyContent);
    }

    // ── PRIVATE: EMBED IMAGES ────────────────────────────────

    private String embedImagesInHtml(String html,
                                     nl.siegmann.epublib.domain.Book epubBook,
                                     Map<String, String> imageUrlToLocalPath) {
        if (html == null || html.isBlank()) return html;
        Matcher matcher = Pattern.compile("src=[\"'](https?://[^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE).matcher(html);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String url       = matcher.group(1);
            String localPath = imageUrlToLocalPath.containsKey(url)
                    ? imageUrlToLocalPath.get(url)
                    : downloadAndEmbedImage(url, epubBook, imageUrlToLocalPath);
            if (localPath != null) {
                matcher.appendReplacement(result,
                        "src=\"" + Matcher.quoteReplacement(localPath) + "\"");
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String downloadAndEmbedImage(String url,
                                         nl.siegmann.epublib.domain.Book epubBook,
                                         Map<String, String> imageUrlToLocalPath) {
        try {
            byte[] imageBytes = fileUtil.downloadFromUrl(url);
            String fileName   = url.substring(url.lastIndexOf('/') + 1);
            if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf('?'));
            String resourcePath = "Images/" + fileName;
            if (epubBook.getResources().getByHref(resourcePath) != null) {
                String hash = Integer.toHexString(url.hashCode());
                String ext  = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
                String base = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
                resourcePath = "Images/" + base + "_" + hash + ext;
            }
            epubBook.getResources().add(new Resource(imageBytes, resourcePath));
            String localPath = "../" + resourcePath;
            imageUrlToLocalPath.put(url, localPath);
            return localPath;
        } catch (Exception e) {
            log.warn("[EpubRebuild] Failed to embed image {}: {}", url, e.getMessage());
            return null;
        }
    }

    // ── PRIVATE: INJECT EDITOR KE KOLOFON ───────────────────

    private String injectEditorIntoColophon(String html, List<String> editors) {
        String editorLine = buildEditorLine(editors);
        String[] markers  = {"</strong><br/><br/>", "</strong><br /><br />", "</strong><br/>\n<br/>"};
        for (String marker : markers) {
            int idx = html.indexOf(marker);
            if (idx != -1) {
                int at = idx + marker.length();
                return html.substring(0, at) + editorLine + html.substring(at);
            }
        }
        int p = html.indexOf("</p>");
        return p == -1 ? html + editorLine : html.substring(0, p) + editorLine + html.substring(p);
    }

    private String buildEditorLine(List<String> editors) {
        if (editors == null || editors.isEmpty()) return "";
        return "<br/><br/>Editor: " + String.join(", ", editors) + "<br/><br/>";
    }

    // ── PRIVATE: POST-PROCESS EPUB ZIP ───────────────────────

    /**
     * Post-process:
     * 1. Baca semua entries dari ZIP epublib
     * 2. Hapus toc.ncx, nav0001.xhtml, sgc-nav.css
     * 3. Bangun OPF baru — manifest + spine dari scratch (urutan benar),
     *    metadata dari existing OPF jika tersedia (hanya update modified + editor baru)
     * 4. Pack ulang
     */
    private byte[] postProcessEpubZip(byte[] epubBytes,
                                      List<String> editors,
                                      Book book,
                                      String existingMetadataXml,
                                      List<String> orderedSlugs) {
        try {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            String opfPath = null;
            String opfDir  = "";

            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(epubBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    byte[] data = zis.readAllBytes();
                    entries.put(entry.getName(), data);
                    if (entry.getName().endsWith(".opf")) {
                        opfPath = entry.getName();
                        int lastSlash = opfPath.lastIndexOf('/');
                        opfDir = lastSlash >= 0 ? opfPath.substring(0, lastSlash + 1) : "";
                    }
                    zis.closeEntry();
                }
            }

            if (opfPath == null) {
                log.warn("[EpubRebuild] No OPF found, skipping post-process");
                return epubBytes;
            }

            log.info("[EpubRebuild] OPF path: {}, dir: {}", opfPath, opfDir);

            entries.entrySet().removeIf(e -> {
                String name = e.getKey();
                return name.endsWith("toc.ncx")
                        || name.endsWith("nav0001.xhtml")
                        || name.endsWith("sgc-nav.css");
            });

            List<String> manifestItems = new ArrayList<>();
            for (String name : entries.keySet()) {
                if (name.equals("mimetype") || name.equals("META-INF/container.xml")) continue;
                if (name.startsWith("META-INF/")) continue;
                if (name.equals(opfPath)) continue;
                manifestItems.add(name);
            }

            final String finalOpfDir = opfDir;
            String newOpf = buildCompleteOpf(book, editors, existingMetadataXml,
                    manifestItems, finalOpfDir, orderedSlugs);
            entries.put(opfPath, newOpf.getBytes(StandardCharsets.UTF_8));
            log.info("[EpubRebuild] OPF rebuilt ({} chars, metadata: {})",
                    newOpf.length(), existingMetadataXml != null ? "preserved from existing" : "built from scratch");

            // Pack ulang
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                // mimetype HARUS pertama dan STORED
                if (entries.containsKey("mimetype")) {
                    byte[] mb = entries.get("mimetype");
                    ZipEntry me = new ZipEntry("mimetype");
                    me.setMethod(ZipEntry.STORED);
                    me.setSize(mb.length);
                    me.setCompressedSize(mb.length);
                    me.setCrc(computeCrc32(mb));
                    zos.putNextEntry(me);
                    zos.write(mb);
                    zos.closeEntry();
                }
                if (entries.containsKey("META-INF/container.xml")) {
                    ZipEntry ze = new ZipEntry("META-INF/container.xml");
                    zos.putNextEntry(ze);
                    zos.write(entries.get("META-INF/container.xml"));
                    zos.closeEntry();
                }
                ZipEntry opfEntry = new ZipEntry(opfPath);
                zos.putNextEntry(opfEntry);
                zos.write(entries.get(opfPath));
                zos.closeEntry();
                for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                    String name = e.getKey();
                    if (name.equals("mimetype")
                            || name.equals("META-INF/container.xml")
                            || name.equals(opfPath)) continue;
                    ZipEntry ze = new ZipEntry(name);
                    zos.putNextEntry(ze);
                    zos.write(e.getValue());
                    zos.closeEntry();
                }
            }

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("[EpubRebuild] Post-process failed: {}", e.getMessage(), e);
            return epubBytes;
        }
    }

    // ── PRIVATE: BUILD COMPLETE OPF ──────────────────────────

    /**
     * Bangun OPF EPUB 3 yang valid.
     *
     * <p><b>Strategi metadata (prioritas):</b>
     * <ol>
     *   <li>Jika {@code existingMetadataXml} tersedia (dari epub Cloudinary):
     *       gunakan sebagai base, hanya update {@code dcterms:modified} dan
     *       tambahkan editor baru yang belum ada.</li>
     *   <li>Jika tidak ada: bangun metadata dari entitas {@code Book}
     *       (fallback untuk epub pertama kali di-generate).</li>
     * </ol>
     *
     * <p>Manifest dan spine selalu dibangun dari scratch dengan urutan spine
     * mengikuti {@code orderedSlugs} = urutan nav/TOC.
     */
    private String buildCompleteOpf(Book book,
                                    List<String> editors,
                                    String existingMetadataXml,
                                    List<String> manifestItems,
                                    String opfDir,
                                    List<String> orderedSlugs) {
        String modified = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<package version=\"3.0\"")
                .append(" unique-identifier=\"BookId\"")
                .append(" xml:lang=\"id\"")
                .append(" prefix=\"ibooks: http://vocabulary.itunes.apple.com/rdf/ibooks/vocabulary-extensions-1.0/\"")
                .append(" xmlns=\"http://www.idpf.org/2007/opf\"")
                .append(">\n");

        // ── METADATA ──────────────────────────────────────────
        if (existingMetadataXml != null) {
            // Preserve metadata existing; hanya update modified + tambah editor baru
            String updatedMeta = updateExistingMetadata(existingMetadataXml, editors, modified);
            sb.append(updatedMeta).append("\n");
        } else {
            // Fallback: bangun dari entitas Book
            sb.append(buildFallbackMetadata(book, editors, modified));
        }

        // ── MANIFEST ──────────────────────────────────────────
        sb.append("  <manifest>\n");

        List<String> xhtmlItems = new ArrayList<>();
        List<String> cssItems   = new ArrayList<>();
        List<String> imageItems = new ArrayList<>();
        List<String> otherItems = new ArrayList<>();
        String navItemHref      = null;
        String coverItemHref    = null;

        for (String fullPath : manifestItems) {
            String href = opfDir.isEmpty() ? fullPath
                    : (fullPath.startsWith(opfDir) ? fullPath.substring(opfDir.length()) : fullPath);
            String lower = href.toLowerCase();
            if (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")) {
                if (href.contains("nav.xhtml")) navItemHref = href;
                else xhtmlItems.add(href);
            } else if (lower.endsWith(".css")) {
                cssItems.add(href);
            } else if (lower.matches(".*\\.(png|jpg|jpeg|gif|svg|webp)")) {
                if (href.contains("cover")) coverItemHref = href;
                else imageItems.add(href);
            } else {
                otherItems.add(href);
            }
        }

        if (coverItemHref != null) {
            sb.append("    <item id=\"").append(hrefToId(coverItemHref)).append("\"")
                    .append(" href=\"").append(coverItemHref).append("\"")
                    .append(" media-type=\"").append(detectMediaType(coverItemHref)).append("\"")
                    .append(" properties=\"cover-image\"/>\n");
        }
        if (navItemHref != null) {
            sb.append("    <item id=\"nav\"")
                    .append(" href=\"").append(navItemHref).append("\"")
                    .append(" media-type=\"application/xhtml+xml\"")
                    .append(" properties=\"nav\"/>\n");
        }

        // XHTML items — urutan mengikuti orderedSlugs
        Map<String, String> slugToHref = new LinkedHashMap<>();
        for (String href : xhtmlItems) slugToHref.put(hrefToId(href), href);

        Set<String> writtenIds = new LinkedHashSet<>();
        for (String slug : orderedSlugs) {
            if (slugToHref.containsKey(slug)) {
                sb.append("    <item id=\"").append(slug).append("\"")
                        .append(" href=\"").append(slugToHref.get(slug)).append("\"")
                        .append(" media-type=\"application/xhtml+xml\"/>\n");
                writtenIds.add(slug);
            }
        }
        for (String href : xhtmlItems) {
            String id = hrefToId(href);
            if (!writtenIds.contains(id)) {
                sb.append("    <item id=\"").append(id).append("\"")
                        .append(" href=\"").append(href).append("\"")
                        .append(" media-type=\"application/xhtml+xml\"/>\n");
            }
        }

        for (String href : cssItems) {
            sb.append("    <item id=\"").append(hrefToId(href)).append("\"")
                    .append(" href=\"").append(href).append("\"")
                    .append(" media-type=\"text/css\"/>\n");
        }
        for (String href : imageItems) {
            sb.append("    <item id=\"").append(hrefToId(href)).append("\"")
                    .append(" href=\"").append(href).append("\"")
                    .append(" media-type=\"").append(detectMediaType(href)).append("\"/>\n");
        }
        for (String href : otherItems) {
            sb.append("    <item id=\"").append(hrefToId(href)).append("\"")
                    .append(" href=\"").append(href).append("\"")
                    .append(" media-type=\"").append(detectMediaType(href)).append("\"/>\n");
        }

        sb.append("  </manifest>\n");

        // ── SPINE (urutan nav/TOC) ─────────────────────────────
        sb.append("  <spine>\n");
        Set<String> spineWritten = new LinkedHashSet<>();
        for (String slug : orderedSlugs) {
            if (slugToHref.containsKey(slug)) {
                sb.append("    <itemref idref=\"").append(slug).append("\"/>\n");
                spineWritten.add(slug);
            }
        }
        for (String href : xhtmlItems) {
            String id = hrefToId(href);
            if (!spineWritten.contains(id)) {
                sb.append("    <itemref idref=\"").append(id).append("\"/>\n");
            }
        }
        if (navItemHref != null) {
            sb.append("    <itemref idref=\"nav\" linear=\"no\"/>\n");
        }
        sb.append("  </spine>\n");
        sb.append("</package>\n");

        return sb.toString();
    }

    // ── PRIVATE: UPDATE METADATA EXISTING ────────────────────

    /**
     * Terima blok {@code <metadata>...</metadata>} dari OPF asli dan:
     * <ol>
     *   <li>Update nilai {@code dcterms:modified} ke waktu sekarang.</li>
     *   <li>Tambahkan editor baru yang belum ada sebagai
     *       {@code dc:contributor} dengan role {@code edt}.</li>
     * </ol>
     * Semua field lain dibiarkan utuh persis seperti aslinya.
     */
    private String updateExistingMetadata(String metadataXml,
                                          List<String> editors,
                                          String modified) {
        // 1. Update dcterms:modified (atau tambahkan jika belum ada)
        if (metadataXml.contains("dcterms:modified")) {
            metadataXml = metadataXml.replaceAll(
                    "<meta\\s+property=\"dcterms:modified\">[^<]*</meta>",
                    "<meta property=\"dcterms:modified\">" + modified + "</meta>"
            );
        } else {
            metadataXml = metadataXml.replace(
                    "</metadata>",
                    "    <meta property=\"dcterms:modified\">" + modified + "</meta>\n  </metadata>"
            );
        }

        // 2. Tambahkan editor baru yang belum ada
        if (editors == null || editors.isEmpty()) return metadataXml;

        // Kumpulkan nama contributor yang sudah ada (case-insensitive)
        Set<String> existingContributors = new HashSet<>();
        Matcher m = Pattern.compile(
                "<dc:contributor[^>]*>([^<]+)</dc:contributor>",
                Pattern.CASE_INSENSITIVE
        ).matcher(metadataXml);
        while (m.find()) {
            existingContributors.add(m.group(1).trim().toLowerCase());
        }

        // Cari editor-id tertinggi yang sudah ada
        int maxEditorId = -1;
        Matcher idMatcher = Pattern.compile("id=\"editor-(\\d+)\"").matcher(metadataXml);
        while (idMatcher.find()) {
            maxEditorId = Math.max(maxEditorId, Integer.parseInt(idMatcher.group(1)));
        }

        // Bangun XML untuk editor baru
        StringBuilder newEditors = new StringBuilder();
        int nextId = maxEditorId + 1;
        for (String editorName : editors) {
            if (!existingContributors.contains(editorName.trim().toLowerCase())) {
                newEditors.append("    <dc:contributor id=\"editor-").append(nextId).append("\">")
                        .append(escapeXml(editorName)).append("</dc:contributor>\n");
                newEditors.append("    <meta refines=\"#editor-").append(nextId)
                        .append("\" property=\"role\" scheme=\"marc:relators\">edt</meta>\n");
                nextId++;
                log.info("[EpubRebuild] Adding new editor to metadata: '{}'", editorName);
            } else {
                log.info("[EpubRebuild] Editor '{}' already in metadata, skipping", editorName);
            }
        }

        if (newEditors.length() > 0) {
            metadataXml = metadataXml.replace(
                    "</metadata>",
                    newEditors + "  </metadata>"
            );
        }

        return metadataXml;
    }

    // ── PRIVATE: FALLBACK METADATA ────────────────────────────

    /**
     * Bangun blok {@code <metadata>} dari entitas {@code Book}.
     * Dipakai hanya jika tidak ada epub existing di Cloudinary
     * (epub pertama kali di-generate).
     */
    private String buildFallbackMetadata(Book book, List<String> editors, String modified) {
        String bookUuid = UUID.randomUUID().toString();
        StringBuilder sb = new StringBuilder();
        sb.append("  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\"")
                .append(" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n");

        sb.append("    <dc:identifier id=\"BookId\">urn:uuid:")
                .append(bookUuid).append("</dc:identifier>\n");
        sb.append("    <dc:title>").append(escapeXml(book.getTitle())).append("</dc:title>\n");
        sb.append("    <dc:language>id</dc:language>\n");

        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            sb.append("    <dc:description>")
                    .append(escapeXml(book.getDescription())).append("</dc:description>\n");
        }

        String publisher = (book.getPublisher() != null && !book.getPublisher().isBlank())
                ? book.getPublisher() : "MasasilaM";
        sb.append("    <dc:publisher>").append(escapeXml(publisher)).append("</dc:publisher>\n");

        if (book.getPublicationYear() != null) {
            sb.append("    <dc:date>").append(book.getPublicationYear()).append("</dc:date>\n");
        } else if (book.getPublishedAt() != null) {
            sb.append("    <dc:date>").append(book.getPublishedAt().toLocalDate()).append("</dc:date>\n");
        }

        if (book.getSource() != null && !book.getSource().isBlank()) {
            sb.append("    <dc:source>").append(escapeXml(book.getSource())).append("</dc:source>\n");
        }

        sb.append("    <dc:type>Text</dc:type>\n");
        sb.append("    <dc:format>application/epub+zip</dc:format>\n");
        sb.append("    <meta property=\"dcterms:modified\">").append(modified).append("</meta>\n");

        if (editors != null) {
            for (int i = 0; i < editors.size(); i++) {
                sb.append("    <dc:contributor id=\"editor-").append(i).append("\">")
                        .append(escapeXml(editors.get(i))).append("</dc:contributor>\n");
                sb.append("    <meta refines=\"#editor-").append(i)
                        .append("\" property=\"role\" scheme=\"marc:relators\">edt</meta>\n");
            }
        }

        sb.append("    <meta name=\"cover\" content=\"cover\"/>\n");
        sb.append("    <meta property=\"ibooks:version\">1.0.0</meta>\n");
        sb.append("  </metadata>\n");
        return sb.toString();
    }

    // ── PRIVATE: OPF HELPER UTILS ─────────────────────────────

    private String hrefToId(String href) {
        String name = href.contains("/") ? href.substring(href.lastIndexOf('/') + 1) : href;
        if (name.contains(".")) name = name.substring(0, name.lastIndexOf('.'));
        name = name.replaceAll("[^a-zA-Z0-9\\-_]", "-");
        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) name = "x" + name;
        return name;
    }

    private String detectMediaType(String href) {
        String lower = href.toLowerCase();
        if (lower.endsWith(".xhtml") || lower.endsWith(".html")) return "application/xhtml+xml";
        if (lower.endsWith(".css"))   return "text/css";
        if (lower.endsWith(".png"))   return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif"))   return "image/gif";
        if (lower.endsWith(".svg"))   return "image/svg+xml";
        if (lower.endsWith(".webp"))  return "image/webp";
        if (lower.endsWith(".ttf"))   return "font/ttf";
        if (lower.endsWith(".otf"))   return "font/otf";
        if (lower.endsWith(".woff"))  return "font/woff";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".mp3"))   return "audio/mpeg";
        if (lower.endsWith(".mp4"))   return "video/mp4";
        return "application/octet-stream";
    }

    // ── PRIVATE: SIMPAN EDITOR KE DATABASE ──────────────────

    private void saveEditorsToDatabase(Long bookId, List<String> editors) {
        if (editors == null || editors.isEmpty()) return;
        for (String editorName : editors) {
            try {
                Contributor existing = contributorMapper.findByName(editorName);
                Long contributorId;
                if (existing != null) {
                    contributorId = existing.getId();
                } else {
                    Contributor c = new Contributor();
                    c.setName(editorName);
                    c.setSlug(generateSlug(editorName));
                    c.setRole("Editor");
                    c.setCreatedAt(LocalDateTime.now());
                    c.setUpdatedAt(LocalDateTime.now());
                    contributorMapper.insertContributor(c);
                    contributorId = c.getId();
                    log.info("[EpubRebuild] New contributor: '{}' (id={})", editorName, contributorId);
                }
                if (contributorMapper.countBookContributor(bookId, contributorId, "Editor") == 0) {
                    contributorMapper.insertBookContributor(bookId, contributorId, "Editor");
                    log.info("[EpubRebuild] book_contributors: book={} <- editor={}", bookId, contributorId);
                }
            } catch (Exception e) {
                log.error("[EpubRebuild] Failed to save editor '{}': {}", editorName, e.getMessage(), e);
            }
        }
    }

    // ── PRIVATE: UTILS ────────────────────────────────────────

    private String detectExtension(String url, String defaultExt) {
        try {
            String path = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.contains(".")) return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        } catch (Exception ignored) {}
        return defaultExt;
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private long computeCrc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

    private String stripTags(String html) {
        return html == null ? "" : html.replaceAll("<[^>]+>", "").trim();
    }

    private String sanitizeForXhtml(String html) {
        if (html == null || html.isBlank()) return "<p></p>";
        return html
                .replaceAll("<br\\s*>", "<br/>")
                .replaceAll("<hr\\s*>", "<hr/>")
                .replaceAll("<img([^>]*)(?<!/)>", "<img$1/>")
                .replaceAll("<input([^>]*)(?<!/)>", "<input$1/>");
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}