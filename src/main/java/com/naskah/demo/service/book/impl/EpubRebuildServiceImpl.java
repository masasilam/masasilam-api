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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpubRebuildServiceImpl implements EpubRebuildService {

    private final BookMapper          bookMapper;
    private final BookChapterMapper   chapterMapper;
    private final CorrectionMapper    correctionMapper;
    private final ContributorMapper   contributorMapper;
    private final FileUtil            fileUtil;

    // ── PUBLIC: ASYNC REBUILD ────────────────────────────────

    @Override
    @Async("rebuildExecutor")
    public void rebuildAsync(Long bookId) {
        log.info("[EpubRebuild] Starting async rebuild for book {}", bookId);
        long startTime = System.currentTimeMillis();

        try {
            // 1. Ambil data buku
            Book book = bookMapper.findById(bookId);
            if (book == null) {
                log.error("[EpubRebuild] Book {} not found, aborting rebuild", bookId);
                return;
            }

            // 2. Ambil semua chapter terurut
            List<BookChapter> chapters = chapterMapper.findChaptersByBookId(bookId);
            if (chapters.isEmpty()) {
                log.warn("[EpubRebuild] Book {} has no chapters, skipping", bookId);
                return;
            }

            log.info("[EpubRebuild] Building epub from {} chapters for book '{}'",
                    chapters.size(), book.getTitle());

            // 3. Ambil daftar editor (user yang approve correction)
            List<String> editors = correctionMapper.findApprovedEditorsByBookId(bookId);
            log.info("[EpubRebuild] Found {} editors for book {}: {}",
                    editors.size(), bookId, editors);

            // 4. Generate file .epub dari DB chapters
            byte[] epubBytes = buildEpubFromChapters(book, chapters, editors);

            log.info("[EpubRebuild] Generated {} bytes for book {}",
                    epubBytes.length, bookId);

            // 5. Inject editor ke content.opf (post-process zip)
            if (!editors.isEmpty()) {
                epubBytes = injectEditorsIntoOpf(epubBytes, editors);
                log.info("[EpubRebuild] Injected {} editors into OPF", editors.size());
            }

            // 6. Simpan editor ke tabel contributors dan book_contributors
            saveEditorsToDatabase(bookId, editors);

            // 7. Upload ke Cloudinary dengan OVERWRITE
            String newFileUrl = fileUtil.uploadEpubOverwrite(epubBytes, book.getSlug());

            // 8. Update book.file_url dan epub_generated_at di DB
            book.setFileUrl(newFileUrl);
            book.setEpubGeneratedAt(LocalDateTime.now());
            bookMapper.updateBook(book);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[EpubRebuild] ✓ Completed for book '{}' in {}ms → {}",
                    book.getTitle(), elapsed, newFileUrl);

        } catch (Exception e) {
            log.error("[EpubRebuild] ✗ Failed for book {}: {}",
                    bookId, e.getMessage(), e);
        }
    }

    // ── PRIVATE: BUILD EPUB ──────────────────────────────────

    private byte[] buildEpubFromChapters(Book book,
                                         List<BookChapter> chapters,
                                         List<String> editors)
            throws IOException {

        nl.siegmann.epublib.domain.Book epubBook =
                new nl.siegmann.epublib.domain.Book();

        // ── Metadata ─────────────────────────────────────────
        epubBook.getMetadata().addTitle(book.getTitle());
        if (book.getDescription() != null) {
            epubBook.getMetadata().addDescription(book.getDescription());
        }

        // ── CSS: load masasilam.css dari classpath ────────────
        // Letakkan file di: src/main/resources/epub/masasilam.css
        try (var cssStream = getClass().getResourceAsStream("/epub/masasilam.css")) {
            if (cssStream != null) {
                Resource cssResource = new Resource(cssStream, "Styles/masasilam.css");
                epubBook.getResources().add(cssResource);
                log.debug("[EpubRebuild] masasilam.css loaded from classpath");
            } else {
                log.warn("[EpubRebuild] masasilam.css not found in classpath, skipping CSS");
            }
        }

        // ── Cover Image ───────────────────────────────────────
        if (book.getCoverImageUrl() != null) {
            try {
                byte[] coverBytes = fileUtil.downloadFromUrl(book.getCoverImageUrl());
                Resource coverResource = new Resource(coverBytes, "Images/cover.jpg");
                epubBook.setCoverImage(coverResource);
                log.debug("[EpubRebuild] Cover embedded successfully");
            } catch (Exception e) {
                log.warn("[EpubRebuild] Could not embed cover: {}", e.getMessage());
            }
        }

        // ── Chapters ──────────────────────────────────────────
        for (BookChapter chapter : chapters) {
            String xhtml = wrapChapterAsXhtml(chapter, editors);
            String resourcePath = "Text/" + chapter.getSlug() + ".xhtml";
            Resource chapterResource = new Resource(
                    xhtml.getBytes(StandardCharsets.UTF_8), resourcePath);
            epubBook.addSection(chapter.getTitle(), chapterResource);
        }

        // ── Serialize ─────────────────────────────────────────
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new EpubWriter().write(epubBook, baos);
        return baos.toByteArray();
    }

    // ── PRIVATE: WRAP CHAPTER ────────────────────────────────

    /**
     * Wrap html_content chapter menjadi XHTML yang valid untuk epub.
     *
     * Strategi:
     * - Jika html_content sudah mengandung epub:type → pakai langsung (titlepage, colophon, dll)
     * - Jika chapter kolofon → inject baris editor sebelum "Sumber:"
     * - Jika chapter biasa → bungkus dengan <section class="chapter">
     */
    private String wrapChapterAsXhtml(BookChapter chapter, List<String> editors) {
        String safeTitle = escapeXml(chapter.getTitle() != null
                ? chapter.getTitle() : "Bab " + chapter.getChapterNumber());

        String sanitizedHtml = sanitizeForXhtml(chapter.getHtmlContent());

        // Deteksi struktur epub di konten
        boolean hasEpubStructure = sanitizedHtml.contains("epub:type=");
        boolean isColophon       = sanitizedHtml.contains("epub:type=\"colophon\"")
                || sanitizedHtml.contains("epub:type='colophon'");

        // Inject editor ke kolofon jika ada
        if (isColophon && editors != null && !editors.isEmpty()) {
            sanitizedHtml = injectEditorIntoColophon(sanitizedHtml, editors);
        }

        String bodyAttrs;
        String bodyContent;

        if (hasEpubStructure) {
            // Konten sudah terstruktur — pakai langsung tanpa wrapper tambahan
            bodyContent = sanitizedHtml;

            if (sanitizedHtml.contains("titlepage")) {
                bodyAttrs = " epub:type=\"frontmatter\"";
            } else if (isColophon
                    || sanitizedHtml.contains("imprint")
                    || sanitizedHtml.contains("copyright-page")) {
                bodyAttrs = " epub:type=\"backmatter\"";
            } else {
                bodyAttrs = "";
            }
        } else {
            // Chapter biasa
            bodyContent = """
                    <section class="chapter" epub:type="chapter">
                        <h1 class="chapter-title">%s</h1>
                        %s
                    </section>
                    """.formatted(safeTitle, sanitizedHtml);
            bodyAttrs = "";
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml"
                      xmlns:epub="http://www.idpf.org/2007/ops"
                      xml:lang="id" lang="id">
                <head>
                    <meta charset="UTF-8"/>
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

    // ── PRIVATE: INJECT EDITOR KE KOLOFON ───────────────────

    /**
     * Sisipkan baris editor ke dalam html_content kolofon.
     *
     * Target posisi: setelah </strong><br/><br/> terakhir dari nama penulis
     * Contoh hasil:
     *   <strong>TAN MALAKA</strong><br/><br/>
     *   <br/><br/>Editor: Fajar Andika<br/><br/>
     *   Sumber: ...
     */
    private String injectEditorIntoColophon(String html, List<String> editors) {
        String editorLine = buildEditorLine(editors);

        // Marker: akhir dari blok nama penulis
        String[] markers = {
                "</strong><br/><br/>",
                "</strong><br /><br />",
                "</strong><br/>\n<br/>"
        };

        for (String marker : markers) {
            int idx = html.indexOf(marker);
            if (idx != -1) {
                int insertAt = idx + marker.length();
                log.debug("[EpubRebuild] Editor line injected into colophon at pos {}", insertAt);
                return html.substring(0, insertAt) + editorLine + html.substring(insertAt);
            }
        }

        // Fallback: sisipkan sebelum </p> pertama
        log.warn("[EpubRebuild] Author marker not found in colophon, " +
                "inserting before first </p>");
        int firstPClose = html.indexOf("</p>");
        if (firstPClose == -1) return html + editorLine;
        return html.substring(0, firstPClose) + editorLine + html.substring(firstPClose);
    }

    /**
     * Build string editor untuk kolofon.
     * 1 editor  → "<br/><br/>Editor: Fajar Andika<br/><br/>"
     * N editor  → "<br/><br/>Editor: Fajar Andika, Joe, Dalam<br/><br/>"
     */
    private String buildEditorLine(List<String> editors) {
        if (editors == null || editors.isEmpty()) return "";
        return "<br/><br/>Editor: " + String.join(", ", editors) + "<br/><br/>";
    }

    // ── PRIVATE: INJECT EDITOR KE OPF (ZIP POST-PROCESS) ────

    /**
     * Post-process bytes epub untuk inject editor ke content.opf.
     * Epub adalah ZIP — buka, modifikasi OPF, pack ulang.
     */
    private byte[] injectEditorsIntoOpf(byte[] epubBytes, List<String> editors) {
        if (editors == null || editors.isEmpty()) return epubBytes;

        try {
            // Baca semua entry dari zip
            Map<String, byte[]> entries = new LinkedHashMap<>();
            String opfPath = null;

            try (ZipInputStream zis = new ZipInputStream(
                    new ByteArrayInputStream(epubBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    byte[] data = zis.readAllBytes();
                    entries.put(entry.getName(), data);
                    if (entry.getName().endsWith(".opf")) {
                        opfPath = entry.getName();
                    }
                    zis.closeEntry();
                }
            }

            if (opfPath == null) {
                log.warn("[EpubRebuild] No OPF found in epub zip, skipping OPF injection");
                return epubBytes;
            }

            // Modifikasi OPF
            String opfContent = new String(entries.get(opfPath), StandardCharsets.UTF_8);
            String modifiedOpf = injectEditorsIntoOpfXml(opfContent, editors);
            entries.put(opfPath, modifiedOpf.getBytes(StandardCharsets.UTF_8));

            log.debug("[EpubRebuild] OPF modified, injected {} editors", editors.size());

            // Pack ulang ke bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                // mimetype HARUS entry pertama dan STORED (tidak dikompresi)
                if (entries.containsKey("mimetype")) {
                    byte[] mimeBytes = entries.get("mimetype");
                    ZipEntry mimeEntry = new ZipEntry("mimetype");
                    mimeEntry.setMethod(ZipEntry.STORED);
                    mimeEntry.setSize(mimeBytes.length);
                    mimeEntry.setCompressedSize(mimeBytes.length);
                    mimeEntry.setCrc(computeCrc32(mimeBytes));
                    zos.putNextEntry(mimeEntry);
                    zos.write(mimeBytes);
                    zos.closeEntry();
                }

                for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                    if (e.getKey().equals("mimetype")) continue;
                    ZipEntry ze = new ZipEntry(e.getKey());
                    zos.putNextEntry(ze);
                    zos.write(e.getValue());
                    zos.closeEntry();
                }
            }

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("[EpubRebuild] Failed to inject editors into OPF: {}",
                    e.getMessage(), e);
            return epubBytes;
        }
    }

    /**
     * Inject <dc:contributor> + <meta refines> ke XML OPF.
     * Hapus dulu hasil inject sebelumnya agar tidak duplikat.
     */
    private String injectEditorsIntoOpfXml(String opfXml, List<String> editors) {
        // Hapus editor lama dari rebuild sebelumnya
        String cleaned = opfXml.replaceAll(
                "(?s)\\s*<dc:contributor id=\"editor-\\d+\">[^<]*</dc:contributor>\\s*" +
                        "<meta refines=\"#editor-\\d+\" property=\"role\"[^/]*/?>",
                ""
        );

        // Build XML editor baru
        StringBuilder editorXml = new StringBuilder();
        for (int i = 0; i < editors.size(); i++) {
            String name = escapeXml(editors.get(i));
            editorXml.append(String.format(
                    "    <dc:contributor id=\"editor-%d\">%s</dc:contributor>\n" +
                            "    <meta refines=\"#editor-%d\" property=\"role\" " +
                            "scheme=\"marc:relators\">edt</meta>\n",
                    i, name, i, i
            ));
        }

        // Sisipkan sebelum </metadata>
        String marker = "</metadata>";
        int idx = cleaned.indexOf(marker);
        if (idx == -1) {
            log.warn("[EpubRebuild] </metadata> not found in OPF");
            return cleaned;
        }

        return cleaned.substring(0, idx) + editorXml + cleaned.substring(idx);
    }

    // ── PRIVATE: SIMPAN EDITOR KE DATABASE ──────────────────

    /**
     * Simpan editor ke tabel contributors dan book_contributors.
     * - Jika contributor sudah ada (by name) → pakai ID yang lama
     * - Jika belum ada → insert baru
     * - Jika relasi book_contributors sudah ada → skip (ON CONFLICT DO NOTHING)
     */
    private void saveEditorsToDatabase(Long bookId, List<String> editors) {
        if (editors == null || editors.isEmpty()) return;

        for (String editorName : editors) {
            try {
                // 1. Cek apakah contributor sudah ada
                Contributor existing = contributorMapper.findByName(editorName);

                Long contributorId;
                if (existing != null) {
                    contributorId = existing.getId();
                    log.debug("[EpubRebuild] Editor '{}' already in contributors (id={})",
                            editorName, contributorId);
                } else {
                    // 2. Insert contributor baru
                    Contributor newContributor = new Contributor();
                    newContributor.setName(editorName);
                    newContributor.setSlug(generateSlug(editorName));
                    newContributor.setRole("Editor");
                    newContributor.setCreatedAt(LocalDateTime.now());
                    newContributor.setUpdatedAt(LocalDateTime.now());
                    contributorMapper.insertContributor(newContributor);
                    contributorId = newContributor.getId();
                    log.info("[EpubRebuild] New contributor inserted: '{}' (id={})",
                            editorName, contributorId);
                }

                // 3. Upsert relasi book_contributors
                int exists = contributorMapper.countBookContributor(
                        bookId, contributorId, "Editor");
                if (exists == 0) {
                    contributorMapper.insertBookContributor(bookId, contributorId, "Editor");
                    log.info("[EpubRebuild] book_contributors: book={} ← editor={}",
                            bookId, contributorId);
                } else {
                    log.debug("[EpubRebuild] book_contributors relation already exists, skipping");
                }

            } catch (Exception e) {
                log.error("[EpubRebuild] Failed to save editor '{}' to DB: {}",
                        editorName, e.getMessage(), e);
            }
        }
    }

    // ── PRIVATE: UTILS ────────────────────────────────────────

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private long computeCrc32(byte[] data) {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(data);
        return crc.getValue();
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