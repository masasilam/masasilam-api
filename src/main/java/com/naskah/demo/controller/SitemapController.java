package com.naskah.demo.controller;

import com.naskah.demo.model.entity.Author;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.model.entity.Genre;
import com.naskah.demo.service.AuthorService;
import com.naskah.demo.service.GenreService;
import com.naskah.demo.service.book.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SitemapController {
    private final BookService bookService;
    private final AuthorService authorService;
    private final GenreService genreService;
    private static final String SITE_URL = "https://masasilam.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    // ============================================
    // SITEMAP INDEX - Entry Point
    // ============================================
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapIndex() {
        String today = LocalDateTime.now().format(DATE_FORMATTER);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
                "  <sitemap>\n" +
                "    <loc>" + SITE_URL + "/sitemap-static.xml</loc>\n" +
                "    <lastmod>" + today + "</lastmod>\n" +
                "  </sitemap>\n" +
                "  <sitemap>\n" +
                "    <loc>" + SITE_URL + "/sitemap-genres.xml</loc>\n" +
                "    <lastmod>" + today + "</lastmod>\n" +
                "  </sitemap>\n" +
                "  <sitemap>\n" +
                "    <loc>" + SITE_URL + "/sitemap-authors.xml</loc>\n" +
                "    <lastmod>" + today + "</lastmod>\n" +
                "  </sitemap>\n" +
                "  <sitemap>\n" +
                "    <loc>" + SITE_URL + "/sitemap-books.xml</loc>\n" +
                "    <lastmod>" + today + "</lastmod>\n" +
                "  </sitemap>\n" +
                "</sitemapindex>";
    }

    // ============================================
    // SITEMAP STATIC - Static Pages
    // ============================================
    @GetMapping(value = "/sitemap-static.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapStatic() {
        String today = LocalDateTime.now().format(DATE_FORMATTER);
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages dengan prioritas sesuai importance
        addUrl(xml, "/", "daily", "1.0", today);
        addUrl(xml, "/buku", "daily", "0.9", today);
        addUrl(xml, "/kategori", "weekly", "0.9", today);
        addUrl(xml, "/penulis", "weekly", "0.9", today);
        addUrl(xml, "/buku/terpopuler", "daily", "0.8", today);
        addUrl(xml, "/buku/terbaru", "daily", "0.8", today);
        addUrl(xml, "/buku/rekomendasi", "weekly", "0.7", today);
        addUrl(xml, "/cari", "weekly", "0.6", today);
        addUrl(xml, "/tentang", "monthly", "0.5", today);
        addUrl(xml, "/cara-membaca", "monthly", "0.5", today);
        addUrl(xml, "/faq", "monthly", "0.5", today);
        addUrl(xml, "/kontak", "monthly", "0.4", today);
        addUrl(xml, "/privasi", "yearly", "0.3", today);
        addUrl(xml, "/syarat-ketentuan", "yearly", "0.3", today);

        xml.append("</urlset>");
        return xml.toString();
    }

    // ============================================
    // SITEMAP GENRES - Dynamic from Database
    // ============================================
    @GetMapping(value = "/sitemap-genres.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapGenres() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        try {
            List<Genre> genres = genreService.getAllGenresWithBooks();
            String today = LocalDateTime.now().format(DATE_FORMATTER);

            for (Genre genre : genres) {
                // Only include genres with at least 1 book
                if (genre.getBookCount() != null && genre.getBookCount() >= 1) {
                    String lastmod = genre.getCreatedAt() != null
                            ? genre.getCreatedAt().toString().substring(0, 10)
                            : today;
                    addUrl(xml, "/kategori/" + genre.getSlug(), "weekly", "0.8", lastmod);
                }
            }
        } catch (Exception e) {
            // Log error but return valid XML
            log.error("Error generating genres sitemap: {}", e.getMessage());
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    // ============================================
    // SITEMAP AUTHORS - Dynamic from Database
    // ============================================
    @GetMapping(value = "/sitemap-authors.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapAuthors() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        try {
            List<Author> authors = authorService.getAllAuthors();

            for (Author author : authors) {
                String lastmod = author.getUpdatedAt() != null
                        ? author.getUpdatedAt().format(DATE_FORMATTER)
                        : (author.getCreatedAt() != null
                        ? author.getCreatedAt().format(DATE_FORMATTER)
                        : LocalDateTime.now().format(DATE_FORMATTER));

                addUrl(xml, "/penulis/" + author.getSlug(), "monthly", "0.7", lastmod);
            }
        } catch (Exception e) {
            log.error("Error generating authors sitemap: {}", e.getMessage());
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    // ============================================
    // SITEMAP BOOKS - Dynamic from Database with Images
    // ============================================
    @GetMapping(value = "/sitemap-books.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapBooks() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" ");
        xml.append("xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">\n");

        try {
            List<Book> books = bookService.getAllBooksForSitemap();

            for (Book book : books) {
                String lastmod = book.getUpdatedAt() != null
                        ? book.getUpdatedAt().format(DATE_FORMATTER)
                        : (book.getCreatedAt() != null
                        ? book.getCreatedAt().format(DATE_FORMATTER)
                        : LocalDateTime.now().format(DATE_FORMATTER));

                // Main book page
                xml.append("  <url>\n");
                xml.append("    <loc>").append(SITE_URL).append("/buku/").append(book.getSlug()).append("</loc>\n");
                xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
                xml.append("    <changefreq>monthly</changefreq>\n");
                xml.append("    <priority>0.8</priority>\n");

                // Add book cover image for better SEO
                if (book.getCoverImageUrl() != null && !book.getCoverImageUrl().isEmpty()) {
                    xml.append("    <image:image>\n");
                    xml.append("      <image:loc>").append(escapeXml(book.getCoverImageUrl())).append("</image:loc>\n");
                    xml.append("      <image:title>").append(escapeXml(book.getTitle())).append("</image:title>\n");
                    xml.append("    </image:image>\n");
                }

                xml.append("  </url>\n");

                // Table of Contents page (daftar-isi)
                addUrl(xml, "/buku/" + book.getSlug() + "/daftar-isi", "yearly", "0.6", lastmod);

                // Reviews page (ulasan)
                addUrl(xml, "/buku/" + book.getSlug() + "/ulasan", "monthly", "0.5", lastmod);
            }
        } catch (Exception e) {
            System.err.println("Error generating books sitemap: " + e.getMessage());
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Helper method to add URL entry to sitemap
     */
    private void addUrl(StringBuilder xml, String path, String changefreq, String priority, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(SITE_URL).append(path).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    /**
     * Escape XML special characters to prevent malformed XML
     */
    private String escapeXml(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}