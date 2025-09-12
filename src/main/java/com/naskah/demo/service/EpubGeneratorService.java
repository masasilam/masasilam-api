//package com.naskah.demo.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import java.io.*;
//import java.nio.file.*;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipOutputStream;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class EpubGeneratorService {
//
//    @Value("${epub.output.directory:./epub-output}")
//    private String epubOutputDirectory;
//
//    @Value("${epub.template.directory:./epub-templates}")
//    private String templateDirectory;
//
//    public String generateEpub(Project project, List<ProjectPage> pages) {
//        try {
//            log.info("Generating EPUB for project: {}", project.getTitle());
//
//            String projectSlug = generateSlug(project.getTitle());
//            String authorSlug = generateSlug(project.getAuthor());
//            String epubFileName = String.format("%s-%s.epub", authorSlug, projectSlug);
//            String epubPath = Paths.get(epubOutputDirectory, epubFileName).toString();
//
//            // Create EPUB structure
//            Path tempDir = createTempEpubStructure(project, pages);
//
//            // Create EPUB zip file
//            createEpubZip(tempDir, epubPath);
//
//            // Validate EPUB
//            validateEpub(epubPath);
//
//            log.info("EPUB generated successfully: {}", epubPath);
//            return epubPath;
//
//        } catch (Exception e) {
//            log.error("Error generating EPUB for project: {}", project.getId(), e);
//            throw new RuntimeException("Failed to generate EPUB", e);
//        }
//    }
//
//    private Path createTempEpubStructure(Project project, List<ProjectPage> pages) throws IOException {
//        Path tempDir = Files.createTempDirectory("epub-" + project.getId());
//
//        // Create directory structure
//        createDirectoryStructure(tempDir);
//
//        // Copy template files
//        copyTemplateFiles(tempDir);
//
//        // Generate content files
//        generateMimetypeFile(tempDir);
//        generateContainerXml(tempDir);
//        generateContentOpf(tempDir, project, pages);
//        generateTocFiles(tempDir, project, pages);
//        generateChapterFiles(tempDir, project, pages);
//        generateStaticPages(tempDir, project);
//
//        return tempDir;
//    }
//
//    private void createDirectoryStructure(Path baseDir) throws IOException {
//        Files.createDirectories(baseDir.resolve("META-INF"));
//        Files.createDirectories(baseDir.resolve("OEBPS"));
//        Files.createDirectories(baseDir.resolve("OEBPS/css"));
//        Files.createDirectories(baseDir.resolve("OEBPS/images"));
//        Files.createDirectories(baseDir.resolve("OEBPS/text"));
//    }
//
//    private void copyTemplateFiles(Path baseDir) throws IOException {
//        // Copy CSS files
//        copyFile(Paths.get(templateDirectory, "css/core.css"),
//                baseDir.resolve("OEBPS/css/core.css"));
//        copyFile(Paths.get(templateDirectory, "css/local.css"),
//                baseDir.resolve("OEBPS/css/local.css"));
//        copyFile(Paths.get(templateDirectory, "css/skah.css"),
//                baseDir.resolve("OEBPS/css/skah.css"));
//
//        // Copy image files
//        copyFile(Paths.get(templateDirectory, "images/logo-skah.png"),
//                baseDir.resolve("OEBPS/images/logo-skah.png"));
//    }
//
//    private void generateMimetypeFile(Path baseDir) throws IOException {
//        Files.writeString(baseDir.resolve("mimetype"), "application/epub+zip");
//    }
//
//    private void generateContainerXml(Path baseDir) throws IOException {
//        String containerXml = """
//            <?xml version="1.0" encoding="UTF-8"?>
//            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
//                <rootfiles>
//                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
//                </rootfiles>
//            </container>
//            """;
//
//        Files.writeString(baseDir.resolve("META-INF/container.xml"), containerXml);
//    }
//
//    private void generateContentOpf(Path baseDir, Project project, List<ProjectPage> pages) throws IOException {
//        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
//        String authorSlug = generateSlug(project.getAuthor());
//        String bookSlug = generateSlug(project.getTitle());
//
//        StringBuilder manifestItems = new StringBuilder();
//        StringBuilder spineItems = new StringBuilder();
//
//        // Add chapter items
//        for (ProjectPage page : pages) {
//            String chapterId = String.format("chapter-%d", page.getPageNumber());
//            String chapterFile = String.format("text/chapter-%d.xhtml", page.getPageNumber());
//
//            manifestItems.append(String.format(
//                    "    <item id=\"%s.xhtml\" href=\"%s\" media-type=\"application/xhtml+xml\"/>\n",
//                    chapterId, chapterFile));
//
//            spineItems.append(String.format(
//                    "    <itemref idref=\"%s.xhtml\"/>\n", chapterId));
//        }
//
//        String contentOpf = generateContentOpfTemplate(project, currentDate, authorSlug, bookSlug,
//                manifestItems.toString(), spineItems.toString());
//
//        Files.writeString(baseDir.resolve("OEBPS/content.opf"), contentOpf);
//    }
//
//    private String generateContentOpfTemplate(Project project, String currentDate, String authorSlug,
//                                              String bookSlug, String manifestItems, String spineItems) {
//        return String.format("""
//            <?xml version="1.0" encoding="utf-8"?>
//            <package version="3.0" unique-identifier="uid" dir="ltr"
//                     prefix="skah: https://skah.org/vocab/1.0" xml:lang="id-ID"
//                     xmlns="http://www.idpf.org/2007/opf">
//              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
//                <dc:identifier id="uid">url:https://skah.org/ebooks/%s/%s</dc:identifier>
//                <dc:date>%s</dc:date>
//                <meta property="dcterms:modified">%s</meta>
//                <dc:rights>Teks sumber dan karya seni dalam ebook ini diyakini berada dalam domain publik Indonesia</dc:rights>
//                <dc:publisher id="publisher">skah.org</dc:publisher>
//
//                <!-- Book Information -->
//                <dc:title id="title">%s</dc:title>
//                <dc:creator id="author">%s</dc:creator>
//                <meta property="role" refines="#author" scheme="marc:relators">aut</meta>
//                <dc:language>id-ID</dc:language>
//                <dc:description>%s</dc:description>
//
//                <!-- Technical -->
//                <meta property="skah:built-with">Digital Library System v1.0</meta>
//              </metadata>
//
//              <manifest>
//                <!-- CSS -->
//                <item id="core.css" href="css/core.css" media-type="text/css"/>
//                <item id="local.css" href="css/local.css" media-type="text/css"/>
//                <item id="skah.css" href="css/skah.css" media-type="text/css"/>
//
//                <!-- Images -->
//                <item id="logo-skah.png" href="images/logo-skah.png" media-type="image/png"/>
//
//                <!-- Navigation -->
//                <item id="toc.xhtml" href="toc.xhtml" media-type="application/xhtml+xml" properties="nav"/>
//                <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
//
//                <!-- Content -->
//                <item id="titlepage.xhtml" href="text/titlepage.xhtml" media-type="application/xhtml+xml"/>
//                <item id="imprint.xhtml" href="text/imprint.xhtml" media-type="application/xhtml+xml"/>
//            %s
//                <item id="colophon.xhtml" href="text/colophon.xhtml" media-type="application/xhtml+xml"/>
//                <item id="uncopyright.xhtml" href="text/uncopyright.xhtml" media-type="application/xhtml+xml"/>
//              </manifest>
//
//              <spine toc="ncx">
//                <itemref idref="titlepage.xhtml"/>
//                <itemref idref="imprint.xhtml"/>
//            %s
//                <itemref idref="colophon.xhtml"/>
//                <itemref idref="uncopyright.xhtml"/>
//              </spine>
//
//              <guide>
//                <reference type="title-page" title="Halaman Judul" href="text/titlepage.xhtml"/>
//              </guide>
//            </package>
//            """,
//                authorSlug, bookSlug, currentDate, currentDate,
//                project.getTitle(), project.getAuthor(), project.getDescription(),
//                manifestItems, spineItems);
//    }
//
//    private void generateTocFiles(Path baseDir, Project project, List<ProjectPage> pages) throws IOException {
//        // Generate toc.xhtml
//        StringBuilder tocItems = new StringBuilder();
//        int counter = 3; // Start after titlepage and imprint
//
//        for (ProjectPage page : pages) {
//            tocItems.append(String.format("""
//                            <li>
//                                <a href="text/chapter-%d.xhtml">Bab %d</a>
//                            </li>
//                """, page.getPageNumber(), page.getPageNumber()));
//        }
//
//        String tocXhtml = String.format("""
//            <?xml version="1.0" encoding="utf-8"?>
//            <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"
//                  epub:prefix="z3998: http://www.daisy.org/z3998/2012/vocab/structure/, skah: https://skah.org/vocab/1.0/" xml:lang="id">
//            <head>
//                <title>Daftar Isi</title>
//                <link href="css/core.css" rel="stylesheet" type="text/css"/>
//                <link href="css/local.css" rel="stylesheet" type="text/css"/>
//                <link href="css/skah.css" rel="stylesheet" type="text/css"/>
//            </head>
//            <body epub:type="frontmatter">
//                <nav id="toc" role="doc-toc" epub:type="toc">
//                    <h2 epub:type="title">Daftar Isi</h2>
//                    <ol>
//                        <li><a href="text/titlepage.xhtml">Halaman Judul</a></li>
//                        <li><a href="text/imprint.xhtml">Imprint</a></li>
//            %s
//                        <li><a href="text/colophon.xhtml">Kolofon</a></li>
//                        <li><a href="text/uncopyright.xhtml">Uncopyright</a></li>
//                    </ol>
//                </nav>
//            </body>
//            </html>
//            """, tocItems.toString());
//
//        Files.writeString(baseDir.resolve("OEBPS/toc.xhtml"), tocXhtml);
//
//        // Generate toc.ncx
//        generateTocNcx(baseDir, project, pages);
//    }
//
//    private void generateTocNcx(Path baseDir, Project project, List<ProjectPage> pages) throws IOException {
//        String authorSlug = generateSlug(project.getAuthor());
//        String bookSlug = generateSlug(project.getTitle());
//
//        StringBuilder navPoints = new StringBuilder();
//        navPoints.append("""
//                <navPoint id="navpoint-1" playOrder="1">
//                    <navLabel><text>Halaman Judul</text></navLabel>
//                    <content src="text/titlepage.xhtml"/>
//                </navPoint>
//                <navPoint id="navpoint-2" playOrder="2">
//                    <navLabel><text>Imprint</text></navLabel>
//                    <content src="text/imprint.xhtml"/>
//                </navPoint>
//            """);
//
//        int playOrder = 3;
//        for (ProjectPage page : pages) {
//            navPoints.append(String.format("""
//                    <navPoint id="navpoint-%d" playOrder="%d">
//                        <navLabel><text>Bab %d</text></navLabel>
//                        <content src="text/chapter-%d.xhtml"/>
//                    </navPoint>
//                """, playOrder, playOrder, page.getPageNumber(), page.getPageNumber()));
//            playOrder++;
//        }
//
//        navPoints.append(String.format("""
//                <navPoint id="navpoint-%d" playOrder="%d">
//                    <navLabel><text>Kolofon</text></navLabel>
//                    <content src="text/colophon.xhtml"/>
//                </navPoint>
//                <navPoint id="navpoint-%d" playOrder="%d">
//                    <navLabel><text>Uncopyright</text></navLabel>
//                    <content src="text/uncopyright.xhtml"/>
//                </navPoint>
//            """, playOrder, playOrder, playOrder + 1, playOrder + 1));
//
//        String tocNcx = String.format("""
//            <?xml version="1.0" encoding="utf-8"?>
//            <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1" xml:lang="id">
//                <head>
//                    <meta content="url:https://skah.org/ebooks/%s/%s" name="dtb:uid"/>
//                    <meta content="1" name="dtb:depth"/>
//                    <meta content="0" name="dtb:totalPageCount"/>
//                    <meta content="0" name="dtb:maxPageNumber"/>
//                </head>
//                <docTitle>
//                    <text>%s</text>
//                </docTitle>
//                <navMap>
//            %s
//                </navMap>
//            </ncx>
//            """, authorSlug, bookSlug, project.getTitle(), navPoints.toString());
//
//        Files.writeString(baseDir.resolve("OEBPS/toc.ncx"), tocNcx);
//    }
//
//    private void generateChapterFiles(Path baseDir, Project project, List<ProjectPage> pages) throws IOException {
//        for (ProjectPage page : pages) {
//            String chapterContent = generateChapterXhtml(page);
//            String fileName = String.format("chapter-%d.xhtml", page.getPageNumber());
//            Files.writeString(baseDir.resolve("OEBPS/text").resolve(fileName), chapterContent);
//        }
//    }
//
//    private String generateChapterXhtml(ProjectPage page) {
//        String content = page.getP3Text() != null ? page.getP3Text() :
//                page.getP2Text() != null ? page.getP2Text() :
//                        page.getP1Text() != null ? page.getP1Text() :
//                                page.getOcrText();
//
//        return String.format("""
//            <?xml version="1.0" encoding="utf-8"?>
//            <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"
//                  epub:prefix="z3998: http://www.daisy.org/z3998/2012/vocab/structure/, skah: https://skah.org/vocab/1.0/" xml:lang="id">
//            <head>
//                <title>Bab %d</title>
//                <link href="../css/core.css" rel="stylesheet" type="text/css"/>
//                <link href="../css/local.css" rel="stylesheet" type="text/css"/>
//                <link href="../css/skah.css" rel="stylesheet" type="text/css"/>
//            </head>
//            <body epub:type="bodymatter z3998:fiction">
//                <section id="chapter-%d" epub:type="chapter">
//                    <hgroup>
//                        <h2 epub:type="ordinal z3998:roman">%s</h2>
//                        <p epub:type="title">Bab %d</p>
//                    </hgroup>
//                    %s
//                </section>
//            </body>
//            </html>
//            """, page.getPageNumber(), page.getPageNumber(),
//                convertToRoman(page.getPageNumber()), page.getPageNumber(),
//                formatContentAsHtml(content));
//    }
//
//    private void generateStaticPages(Path baseDir, Project project) throws IOException {
//        // Generate titlepage.xhtml
//        String titlepageXhtml = String.format("""
//            <?xml version="1.0" encoding="utf-8"?>
//            <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"
//                  epub:prefix="skah: https://skah.org/vocab/1.0/" xml:lang="id">
//            <head>
//                <title>Halaman Judul</title>
//                <link href="../css/core.css" rel="stylesheet" type="text/css"/>
//                <link href="../css/local.css" rel="stylesheet" type="text/css"/>
//                <link href="../css/skah.css" rel="stylesheet" type="text/css"/>
//            </head>
//            <body epub:type="frontmatter">
//                <section id="titlepage" epub:type="titlepage">
//                    <h1 epub:type="fulltitle">%s</h1>
//                    <p epub:type="z3998:author">%s</p>
//                </section>
//            </body>
//            </html>
//            """, project.getTitle(), project.getAuthor());
//
//        Files.writeString(baseDir.resolve("OEBPS/text/titlepage.xhtml"), titlepageXhtml);
//
//        // Generate other static pages
//        generateImprintPage(baseDir, project);
//        generateColophonPage(baseDir, project);
//        generateUncopyrightPage(baseDir);
//    }
//
//    private void generateImprintPage(Path baseDir, Project project) throws IOException {
//        String imprintXhtml = String.format("""
//            <?xml version="1.0" encoding="utf-8"?>
//            <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"
//                  epub:prefix="skah: https://skah.org/vocab/1.0/" xml:lang="id">
//            <head>
//                <title>Imprint</title>
//                <link href="../css/core.css" rel="stylesheet" type="text/css"/>
//                <link href="../css/local.css" rel="stylesheet" type="text/css"/>
//                <link href="../css/skah.css" rel="stylesheet" type="text/css"/>
//            </head>
//            <body epub:type="frontmatter">
//                <section id="imprint" epub:type="imprint">
//                    <header>
//                        <h2 epub:type="title">Imprint</h2>
//                    </header>
//                    <p>Ebook ini diproduksi untuk <a href="https://skah.org">Skah.org</a><br/>
//                    dan didasarkan pada teks yang telah diproses melalui<br/>
//                    sistem kolaboratif perpustakaan digital.</p>
//                    <p>Proyek: %s<br/>
//                    Pengarang: %s</p>
//                </section>
//            </body>
//            </html>
//            """, project.getTitle(), project.getAuthor());
//
//        Files.writeString(baseDir.resolve("OEBPS/text/imprint.xhtml"), imprintXhtml);
//    }
//
//    private void generateColophonPage(Path baseDir, Project project) throws IOException {
//        String colophonXhtml = String.format("""
//            <?xml version="1.0" encoding="utf-8"?>
//            <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"
//                  epub:prefix="z3998: http://www.daisy.org/z3998/2012/vocab/structure/, skah: https://skah.org/vocab/1.0/" xml:lang="id">
//            <head>
//                <title>Kolofon</title>
//                <link href="../css/core.css" rel="stylesheet" type="text/css"/>
//                <link href="../css/local.css" rel="stylesheet" type="text/css"/>
//                <link href="../css/skah.css" rel="stylesheet" type="text/css"/>
//            </head>
//            <body epub:type="backmatter">
//                <section id="colophon" epub:type="colophon">
//                    <header>
//                        <h2 epub:type="title">Kolofon</h2>
//                        <img alt="Logo Skah.org" src="../images/logo-skah.png" epub:type="skah:publisher-logo"/>
//                    </header>
//                    <p><i epub:type="skah:name-title">%s</i><br/>
//                    oleh <a href="#">%s</a>.</p>
//                    <p>Ebook ini diproduksi untuk<br/>
//                    <a href="https://skah.org"><b>Skah.org</b></a><br/>
//                    melalui sistem kolaboratif relawan.</p>
//                    <p>Organisasi sukarelawan<br/>
//                    <a href="https://skah.org">Skah.org</a><br/>
//                    memproduksi dan mendistribusikan ebook berkualitas tinggi<br/>
//                    dari domain publik dan karya berlisensi CC.</p>
//                </section>
//            </body>
//            </html>
//            """, project.getTitle(), project.getAuthor());
//
//        Files.writeString(baseDir.resolve("OEBPS/text/colophon.xhtml"), colophonXhtml);
//    }
//
//    private void generateUncopyrightPage(Path baseDir) throws IOException {
//        String uncopyrightXhtml = """
//            <?xml version="1.0" encoding="utf-8"?>
//            <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"
//                  epub:prefix="z3998: http://www.daisy.org/z3998/2012/vocab/structure/, skah: https://skah.org/vocab/1.0/" xml:lang="id">
//            <head>
//                <title>Uncopyright</title>
//                <link href="../css/core.css" rel="stylesheet" type="text/css"/>
//                <link href="../css/local.css" rel="stylesheet" type="text/css"/>
//                <link href="../css/skah.css" rel="stylesheet" type="text/css"/>
//            </head>
//            <body epub:type="backmatter">
//                <section id="uncopyright" epub:type="copyright-page">
//                    <h2 epub:type="title">Uncopyright</h2>
//                    <blockquote>
//                        <p>Teks sumber dan karya seni dalam ebook ini diyakini berada dalam domain publik Indonesia.</p>
//                        <p>Para pencipta dan kontributor ebook ini mendedikasikan kontribusi mereka ke domain publik
//                        di seluruh dunia melalui ketentuan dalam
//                        <a href="https://creativecommons.org/publicdomain/zero/1.0/">CC0 1.0 Universal Public Domain Dedication</a>.</p>
//                    </blockquote>
//                    <p>Singkatnya, Anda dapat melakukan apa pun dengan ebook ini kecuali memperoleh uang darinya
//                    atau menghapus informasi kredit dari Skah.org.</p>
//                </section>
//            </body>
//            </html>
//            """;
//
//        Files.writeString(baseDir.resolve("OEBPS/text/uncopyright.xhtml"), uncopyrightXhtml);
//    }
//
//    private void createEpubZip(Path sourceDir, String outputPath) throws IOException {
//        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputPath))) {
//            // Add mimetype first (uncompressed)
//            addMimetypeToZip(zos, sourceDir);
//
//            // Add other files
//            Files.walk(sourceDir)
//                    .filter(Files::isRegularFile)
//                    .filter(path -> !path.getFileName().toString().equals("mimetype"))
//                    .forEach(path -> {
//                        try {
//                            String entryName = sourceDir.relativize(path).toString().replace("\\", "/");
//                            ZipEntry entry = new ZipEntry(entryName);
//                            zos.putNextEntry(entry);
//                            Files.copy(path, zos);
//                            zos.closeEntry();
//                        } catch (IOException e) {
//                            throw new RuntimeException("Error adding file to EPUB", e);
//                        }
//                    });
//        }
//    }
//
//    private void addMimetypeToZip(ZipOutputStream zos, Path sourceDir) throws IOException {
//        ZipEntry mimetypeEntry = new ZipEntry("mimetype");
//        mimetypeEntry.setMethod(ZipEntry.STORED);
//
//        byte[] mimetypeBytes = "application/epub+zip".getBytes();
//        mimetypeEntry.setSize(mimetypeBytes.length);
//        mimetypeEntry.setCrc(calculateCrc32(mimetypeBytes));
//
//        zos.putNextEntry(mimetypeEntry);
//        zos.write(mimetypeBytes);
//        zos.closeEntry();
//    }
//
//    private long calculateCrc32(byte[] data) {
//        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
//        crc.update(data);
//        return crc.getValue();
//    }
//
//    private void validateEpub(String epubPath) {
//        // TODO: Integrate with EPUBCheck for validation
//        log.info("EPUB validation for: {}", epubPath);
//    }
//
//    private void copyFile(Path source, Path target) throws IOException {
//        if (Files.exists(source)) {
//            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
//        }
//    }
//
//    private String generateSlug(String text) {
//        return text.toLowerCase()
//                .replaceAll("[^a-z0-9\\s-]", "")
//                .replaceAll("\\s+", "-")
//                .replaceAll("-+", "-")
//                .replaceAll("^-|-$", "");
//    }
//
//    private String convertToRoman(int number) {
//        String[] romanNumerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
//                "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
//        return number <= 20 ? romanNumerals[number] : String.valueOf(number);
//    }
//
//    private String formatContentAsHtml(String content) {
//        if (content == null || content.trim().isEmpty()) {
//            return "<p>Konten tidak tersedia</p>";
//        }
//
//        // Basic paragraph formatting
//        String[] paragraphs = content.split("\n\n");
//        StringBuilder html = new StringBuilder();
//
//        for (String paragraph : paragraphs) {
//            if (!paragraph.trim().isEmpty()) {
//                html.append("<p>").append(escapeHtml(paragraph.trim())).append("</p>\n");
//            }
//        }
//
//        return html.toString();
//    }
//
//    private String escapeHtml(String text) {
//        return text.replace("&", "&amp;")
//                .replace("<", "&lt;")
//                .replace(">", "&gt;")
//                .replace("\"", "&quot;")
//                .replace("'", "&#39;");
//    }
//}
