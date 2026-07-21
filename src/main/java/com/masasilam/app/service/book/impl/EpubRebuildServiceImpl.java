package com.masasilam.app.service.book.impl;

import com.masasilam.app.mapper.book.BookChapterMapper;
import com.masasilam.app.mapper.book.BookMapper;
import com.masasilam.app.mapper.author.ContributorMapper;
import com.masasilam.app.mapper.collaboration.CorrectionMapper;
import com.masasilam.app.model.entity.Book;
import com.masasilam.app.model.entity.BookChapter;
import com.masasilam.app.model.entity.Contributor;
import com.masasilam.app.service.book.EpubRebuildService;
import com.masasilam.app.util.file.FileUtil;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpubRebuildServiceImpl implements EpubRebuildService {
    private final BookMapper bookMapper;
    private final BookChapterMapper chapterMapper;
    private final CorrectionMapper correctionMapper;
    private final ContributorMapper contributorMapper;
    private final FileUtil fileUtil;

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

            log.info("[EpubRebuild] Building epub from {} chapters for book '{}'", chapters.size(), book.getTitle());
            String existingMetadataXml = null;
            if (book.getFileUrl() != null && !book.getFileUrl().isBlank()) {
                try {
                    byte[] existingEpub = fileUtil.downloadFromUrl(book.getFileUrl());
                    existingMetadataXml = extractMetadataXmlFromEpub(existingEpub);
                    if (existingMetadataXml != null) {
                        log.info("[EpubRebuild] Existing metadata loaded from Cloudinary epub");
                    }
                } catch (Exception e) {
                    log.warn("[EpubRebuild] Could not load existing epub for metadata (will build from scratch): {}", e.getMessage());
                }
            }

            List<String> editors = correctionMapper.findApprovedEditorsByBookId(bookId);
            log.info("[EpubRebuild] editors={}", editors.size());

            List<String> orderedSlugs = new ArrayList<>();
            byte[] epubBytes = buildEpubFromChapters(book, chapters, editors, orderedSlugs);
            log.info("[EpubRebuild] Raw epub generated: {} bytes", epubBytes.length);

            epubBytes = postProcessEpubZip(epubBytes, editors, book, existingMetadataXml, orderedSlugs);
            log.info("[EpubRebuild] Post-process done: {} bytes", epubBytes.length);

            saveEditorsToDatabase(bookId, editors);

            String newFileUrl = fileUtil.uploadEpubOverwrite(epubBytes, book.getSlug());

            book.setFileUrl(newFileUrl);
            book.setEpubGeneratedAt(LocalDateTime.now());
            bookMapper.updateBook(book);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[EpubRebuild] Done for '{}' in {}ms -> {}", book.getTitle(), elapsed, newFileUrl);

        } catch (Exception e) {
            log.error("[EpubRebuild] Failed for book {}: {}", bookId, e.getMessage(), e);
        }
    }

    private record XhtmlSection(String slug, String title, String htmlContent, String chapterEpubType) {
    }

    private String extractMetadataXmlFromEpub(byte[] epubBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(epubBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".opf")) {
                    String opfContent = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    Matcher m = Pattern.compile("(<metadata\\b[^>]*>.*?</metadata>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(opfContent);
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

    private byte[] buildEpubFromChapters(Book book, List<BookChapter> chapters, List<String> editors, List<String> orderedSlugs) throws IOException {
        nl.siegmann.epublib.domain.Book epubBook = new nl.siegmann.epublib.domain.Book();
        epubBook.getMetadata().addTitle(book.getTitle());
        if (book.getDescription() != null) {
            epubBook.getMetadata().addDescription(book.getDescription());
        }

        try (var cssStream = getClass().getResourceAsStream("/epub/masasilam.css")) {
            if (cssStream != null) {
                epubBook.getResources().add(new Resource(cssStream, "Styles/masasilam.css"));
            } else {
                log.warn("[EpubRebuild] masasilam.css not found in classpath");
            }
        }

        if (book.getCoverImageUrl() != null) {
            try {
                byte[] coverBytes = fileUtil.downloadFromUrl(book.getCoverImageUrl());
                String coverExt = detectExtension(book.getCoverImageUrl());
                String coverPath = "Images/cover." + coverExt;
                epubBook.setCoverImage(new Resource(coverBytes, coverPath));
            } catch (Exception e) {
                log.warn("[EpubRebuild] Could not embed cover: {}", e.getMessage());
            }
        }

        Map<String, String> imageUrlToLocalPath = new HashMap<>();

        List<XhtmlSection> allSections = new ArrayList<>();
        for (BookChapter chapter : chapters) {
            String processedHtml = embedImagesInHtml(chapter.getHtmlContent(), epubBook, imageUrlToLocalPath);
            allSections.addAll(splitChapterByH1(chapter, processedHtml));
        }
        log.info("[EpubRebuild] Total xhtml sections after h1-split: {}", allSections.size());

        String navXhtml = buildNavXhtml(allSections);
        epubBook.getResources().add(new Resource(navXhtml.getBytes(StandardCharsets.UTF_8), "Text/nav.xhtml"));

        for (XhtmlSection section : allSections) {
            orderedSlugs.add(section.slug());
            String xhtml = wrapSectionAsXhtml(section, editors);
            String resourcePath = "Text/" + section.slug() + ".xhtml";
            epubBook.addSection(section.title(), new Resource(xhtml.getBytes(StandardCharsets.UTF_8), resourcePath));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new EpubWriter().write(epubBook, baos);
        return baos.toByteArray();
    }

    private List<XhtmlSection> splitChapterByH1(BookChapter chapter, String html) {
        if (html == null || html.isBlank()) {
            return List.of(new XhtmlSection(chapter.getSlug(), chapter.getTitle(), "<p></p>", null));
        }

        String epubType = null;
        Matcher etm = Pattern.compile("epub:type=[\"']([^\"']+)[\"']").matcher(html);
        if (etm.find()) epubType = etm.group(1);

        if (epubType != null) {
            return List.of(new XhtmlSection(chapter.getSlug(), chapter.getTitle(), html, epubType));
        }

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
            int[] pos = h1Positions.getFirst();
            String h1Text = stripTags(html.substring(pos[2], pos[3])).trim();
            String title = !h1Text.isBlank() ? h1Text : chapter.getTitle();
            return List.of(new XhtmlSection(chapter.getSlug(), title, html, null));
        }

        log.info("[EpubRebuild] Chapter '{}' has {} h1 headings — splitting", chapter.getSlug(), h1Positions.size());

        List<XhtmlSection> sections = new ArrayList<>();
        for (int i = 0; i < h1Positions.size(); i++) {
            int[] pos = h1Positions.get(i);
            int contentEnd = (i + 1 < h1Positions.size()) ? h1Positions.get(i + 1)[0] : html.length();
            String h1Text = stripTags(html.substring(pos[2], pos[3])).trim();
            String title = !h1Text.isBlank() ? h1Text : chapter.getTitle() + " (" + (i + 1) + ")";
            String content = html.substring(pos[0], contentEnd);
            String slug = (i == 0) ? chapter.getSlug() : chapter.getSlug() + "-s" + (i + 1);
            sections.add(new XhtmlSection(slug, title, content, null));
        }
        return sections;
    }

    private String buildNavXhtml(List<XhtmlSection> sections) {
        StringBuilder tocItems = new StringBuilder();
        StringBuilder landmarkItems = new StringBuilder();
        boolean bodyStartSet = false;
        boolean inBab = false;
        StringBuilder pasalBuffer = new StringBuilder();

        for (XhtmlSection section : sections) {
            String href = section.slug() + ".xhtml";
            String title = escapeXml(section.title());
            String slug = section.slug();
            boolean isBab = (slug.matches("bab-[ivxIVX\\d]+-.*") || slug.startsWith("bab-")) && !slug.matches(".*-s\\d+$");
            boolean isPasal = slug.startsWith("pasal-") && !slug.matches(".*-s\\d+$");
            boolean isSplit = slug.matches(".*-s\\d+$");

            if (isBab) {
                if (inBab) {
                    flushPasalBuffer(tocItems, pasalBuffer);
                    tocItems.append("      </li>\n");
                    pasalBuffer = new StringBuilder();
                }
                tocItems.append("      <li><a href=\"").append(href).append("\">").append(title).append("</a>\n");
                inBab = true;
            } else if ((isPasal || isSplit) && inBab) {
                pasalBuffer.append("          <li><a href=\"").append(href).append("\">").append(title).append("</a></li>\n");
            } else {
                if (inBab) {
                    flushPasalBuffer(tocItems, pasalBuffer);
                    tocItems.append("      </li>\n");
                    pasalBuffer = new StringBuilder();
                    inBab = false;
                }
                tocItems.append("      <li><a href=\"").append(href).append("\">").append(title).append("</a></li>\n");
            }

            String epubType = section.chapterEpubType() != null ? section.chapterEpubType() : "";
            boolean isTitlePage = epubType.contains("titlepage");
            boolean isColophon = epubType.contains("colophon");
            boolean isFront = slug.equals("halaman-judul") || slug.equals("kolofon") || slug.equals("uncopyright");

            if (isTitlePage) {
                landmarkItems.append("      <li><a epub:type=\"cover\" href=\"").append(href).append("\">Sampul</a></li>\n");
            } else if (isColophon) {
                landmarkItems.append("      <li><a epub:type=\"frontmatter\" href=\"").append(href).append("\">Kolofon</a></li>\n");
            } else if (!bodyStartSet && !isFront) {
                landmarkItems.append("      <li><a epub:type=\"bodymatter\" href=\"").append(href).append("\">Mulai Membaca</a></li>\n");
                bodyStartSet = true;
            }
        }

        if (inBab) {
            flushPasalBuffer(tocItems, pasalBuffer);
            tocItems.append("      </li>\n");
        }
        landmarkItems.append("      <li><a epub:type=\"toc\" href=\"nav.xhtml\">Daftar Isi</a></li>\n");

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
        if (!buf.isEmpty()) {
            toc.append("        <ol>\n").append(buf).append("        </ol>\n");
        }
    }

    private String wrapSectionAsXhtml(XhtmlSection section, List<String> editors) {
        String safeTitle = escapeXml(section.title());
        String sanitizedHtml = sanitizeForXhtml(section.htmlContent());
        String epubType = section.chapterEpubType();
        boolean hasEpubType = epubType != null && !epubType.isBlank();
        boolean isColophon = hasEpubType && epubType.contains("colophon");

        if (isColophon && editors != null && !editors.isEmpty()) {
            sanitizedHtml = injectEditorIntoColophon(sanitizedHtml, editors);
        }

        String bodyAttrs;
        String bodyContent;

        if (hasEpubType) {
            bodyContent = sanitizedHtml;
            if (epubType.contains("titlepage")) {
                bodyAttrs = " epub:type=\"frontmatter\"";
            } else if (isColophon || epubType.contains("imprint") || epubType.contains("copyright-page")) {
                bodyAttrs = " epub:type=\"backmatter\"";
            } else {
                bodyAttrs = "";
            }
        } else {
            bodyContent = "<section class=\"chapter\" epub:type=\"chapter\">\n    " + sanitizedHtml + "\n</section>";
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

    private String embedImagesInHtml(String html, nl.siegmann.epublib.domain.Book epubBook, Map<String, String> imageUrlToLocalPath) {
        if (html == null || html.isBlank()) return html;
        Matcher matcher = Pattern.compile("src=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String url = matcher.group(1);
            String localPath = imageUrlToLocalPath.containsKey(url) ? imageUrlToLocalPath.get(url) : downloadAndEmbedImage(url, epubBook, imageUrlToLocalPath);
            if (localPath != null) {
                matcher.appendReplacement(result, "src=\"" + Matcher.quoteReplacement(localPath) + "\"");
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String downloadAndEmbedImage(String url, nl.siegmann.epublib.domain.Book epubBook, Map<String, String> imageUrlToLocalPath) {
        try {
            byte[] imageBytes = fileUtil.downloadFromUrl(url);
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf('?'));
            String resourcePath = "Images/" + fileName;
            if (epubBook.getResources().getByHref(resourcePath) != null) {
                String hash = Integer.toHexString(url.hashCode());
                String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
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

    private String injectEditorIntoColophon(String html, List<String> editors) {
        String editorLine = buildEditorLine(editors);
        String[] markers = {"</strong><br/><br/>", "</strong><br /><br />", "</strong><br/>\n<br/>"};
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

    private byte[] postProcessEpubZip(byte[] epubBytes, List<String> editors, Book book, String existingMetadataXml, List<String> orderedSlugs) {
        try {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            String opfPath = null;
            String opfDir = "";

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
                return name.endsWith("toc.ncx") || name.endsWith("nav0001.xhtml") || name.endsWith("sgc-nav.css");
            });

            List<String> manifestItems = new ArrayList<>();
            for (String name : entries.keySet()) {
                if (name.equals("mimetype") || name.equals("META-INF/container.xml")) continue;
                if (name.startsWith("META-INF/")) continue;
                if (name.equals(opfPath)) continue;
                manifestItems.add(name);
            }

            final String finalOpfDir = opfDir;
            String newOpf = buildCompleteOpf(book, editors, existingMetadataXml, manifestItems, finalOpfDir, orderedSlugs);
            entries.put(opfPath, newOpf.getBytes(StandardCharsets.UTF_8));
            log.info("[EpubRebuild] OPF rebuilt ({} chars, metadata: {})", newOpf.length(), existingMetadataXml != null ? "preserved from existing" : "built from scratch");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
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
                    if (name.equals("mimetype") || name.equals("META-INF/container.xml") || name.equals(opfPath))
                        continue;
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

    private String buildCompleteOpf(Book book, List<String> editors, String existingMetadataXml, List<String> manifestItems, String opfDir, List<String> orderedSlugs) {
        String modified = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<package version=\"3.0\"")
                .append(" unique-identifier=\"BookId\"")
                .append(" xml:lang=\"id\"")
                .append(" prefix=\"ibooks: http://vocabulary.itunes.apple.com/rdf/ibooks/vocabulary-extensions-1.0/\"")
                .append(" xmlns=\"http://www.idpf.org/2007/opf\"")
                .append(">\n");

        if (existingMetadataXml != null) {
            String updatedMeta = updateExistingMetadata(existingMetadataXml, editors, modified);
            sb.append(updatedMeta).append("\n");
        } else {
            sb.append(buildFallbackMetadata(book, editors, modified));
        }

        sb.append("  <manifest>\n");

        List<String> xhtmlItems = new ArrayList<>();
        List<String> cssItems = new ArrayList<>();
        List<String> imageItems = new ArrayList<>();
        List<String> otherItems = new ArrayList<>();
        String navItemHref = null;
        String coverItemHref = null;

        for (String fullPath : manifestItems) {
            String href = opfDir.isEmpty() ? fullPath : (fullPath.startsWith(opfDir) ? fullPath.substring(opfDir.length()) : fullPath);
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

    private String updateExistingMetadata(String metadataXml, List<String> editors, String modified) {
        if (metadataXml.contains("dcterms:modified")) {
            metadataXml = metadataXml.replaceAll("<meta\\s+property=\"dcterms:modified\">[^<]*</meta>", "<meta property=\"dcterms:modified\">" + modified + "</meta>");
        } else {
            metadataXml = metadataXml.replace("</metadata>", "    <meta property=\"dcterms:modified\">" + modified + "</meta>\n  </metadata>");
        }

        if (editors == null || editors.isEmpty()) return metadataXml;

        Set<String> existingContributors = new HashSet<>();
        Matcher m = Pattern.compile("<dc:contributor[^>]*>([^<]+)</dc:contributor>", Pattern.CASE_INSENSITIVE).matcher(metadataXml);
        while (m.find()) {
            existingContributors.add(m.group(1).trim().toLowerCase());
        }

        int maxEditorId = -1;
        Matcher idMatcher = Pattern.compile("id=\"editor-(\\d+)\"").matcher(metadataXml);
        while (idMatcher.find()) {
            maxEditorId = Math.max(maxEditorId, Integer.parseInt(idMatcher.group(1)));
        }

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

        if (!newEditors.isEmpty()) {
            metadataXml = metadataXml.replace("</metadata>", newEditors + "  </metadata>");
        }

        return metadataXml;
    }

    private String buildFallbackMetadata(Book book, List<String> editors, String modified) {
        String bookUuid = UUID.randomUUID().toString();
        StringBuilder sb = new StringBuilder();
        sb.append("  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\"")
                .append(" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n");

        sb.append("    <dc:identifier id=\"BookId\">urn:uuid:").append(bookUuid).append("</dc:identifier>\n");
        sb.append("    <dc:title>").append(escapeXml(book.getTitle())).append("</dc:title>\n");
        sb.append("    <dc:language>id</dc:language>\n");

        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            sb.append("    <dc:description>").append(escapeXml(book.getDescription())).append("</dc:description>\n");
        }

        String publisher = (book.getPublisher() != null && !book.getPublisher().isBlank()) ? book.getPublisher() : "MasasilaM";
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
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".otf")) return "font/otf";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }

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

    private String detectExtension(String url) {
        try {
            String path = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.contains(".")) return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        } catch (Exception ignored) {
        }
        return "png";
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