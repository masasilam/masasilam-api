package com.naskah.demo.util.file;

import com.naskah.demo.model.dto.CompleteEpubMetadata;
import com.naskah.demo.model.dto.AuthorMetadata;
import com.naskah.demo.model.dto.ContributorMetadata;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class EpubMetadataExtractor {

    public static CompleteEpubMetadata extractCompleteMetadata(InputStream epubInputStream) {
        CompleteEpubMetadata metadata = new CompleteEpubMetadata();

        try {
            EpubReader reader = new EpubReader();
            Book book = reader.readEpub(epubInputStream);
            Metadata epubMetadata = book.getMetadata();

            // ✅ FIX 1: Extract title and subtitle properly
            extractTitleAndSubtitle(book, metadata);

            // Extract authors
            List<AuthorMetadata> authors = new ArrayList<>();
            epubMetadata.getAuthors().forEach(author -> {
                AuthorMetadata authorMeta = new AuthorMetadata();
                String firstName = author.getFirstname();
                String lastName = author.getLastname();

                if (firstName != null && lastName != null) {
                    authorMeta.setName(firstName + " " + lastName);
                } else if (firstName != null) {
                    authorMeta.setName(firstName);
                } else if (lastName != null) {
                    authorMeta.setName(lastName);
                } else {
                    authorMeta.setName("Unknown Author");
                }

                authorMeta.setRole("Author");
                authors.add(authorMeta);
            });
            metadata.setAuthors(authors);

            // ✅ FIX: Extract contributors with proper roles from OPF
            List<ContributorMetadata> contributors = extractContributorsWithRoles(book);
            metadata.setContributors(contributors);

            // Extract publication info
            metadata.setPublisher(epubMetadata.getPublishers().isEmpty()
                    ? null : epubMetadata.getPublishers().get(0));

            metadata.setLanguage(epubMetadata.getLanguage());
            metadata.setDescription(epubMetadata.getDescriptions().isEmpty()
                    ? null : epubMetadata.getDescriptions().get(0));

            // ✅ FIX 2: Extract updated_at from dcterms:modified
            metadata.setUpdatedAt(extractModifiedDate(book));

            // Extract publication date
            LocalDate pubDate = extractPublicationDate(epubMetadata);
            metadata.setPublishedAt(pubDate);
            if (pubDate != null) {
                metadata.setPublicationYear(pubDate.getYear());
            }

            // Extract subjects as genres
            List<String> subjects = new ArrayList<>();
            if (epubMetadata.getSubjects() != null && !epubMetadata.getSubjects().isEmpty()) {
                for (String subject : epubMetadata.getSubjects()) {
                    if (subject != null && !subject.isEmpty()) {
                        subjects.add(subject);
                    }
                }
            }
            metadata.setSubjects(subjects);

            // Extract rights/copyright
            String rights = epubMetadata.getRights().isEmpty()
                    ? null : epubMetadata.getRights().get(0);
            metadata.setCopyrightStatus(parseCopyrightStatus(rights));

            // ✅ Extract source (dc:source)
            String source = extractSource(book);
            metadata.setSource(source);
            if (source != null) {
                log.info("Extracted dc:source: {}", source);
            }

            // Extract cover image
            if (book.getCoverImage() != null) {
                metadata.setCoverImageData(book.getCoverImage().getData());
            }

            // Extract category from subjects
            if (!subjects.isEmpty()) {
                metadata.setCategory(subjects.get(0));
            }

        } catch (Exception e) {
            log.error("Failed to extract EPUB metadata: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract EPUB metadata", e);
        }

        return metadata;
    }

    /**
     * ✅ FIX 1: Extract title and subtitle from HTML title page
     */
    private static void extractTitleAndSubtitle(Book book, CompleteEpubMetadata metadata) {
        try {
            // First try to find title page in spine
            for (var ref : book.getSpine().getSpineReferences()) {
                var resource = ref.getResource();
                String content = new String(resource.getData(), "UTF-8");

                if (content.contains("titlepage") || content.contains("epub:type=\"titlepage\"")) {
                    // Parse HTML to extract title and subtitle
                    org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(content);

                    // Look for h1 as title
                    org.jsoup.nodes.Element h1 = doc.select("h1").first();
                    if (h1 != null) {
                        metadata.setTitle(h1.text().trim());
                    }

                    // Look for subtitle (h2 with class subtitle or sigil_not_in_toc)
                    org.jsoup.nodes.Element h2 = doc.select("h2.subtitle, h2.sigil_not_in_toc").first();
                    if (h2 != null) {
                        metadata.setSubtitle(h2.text().trim());
                    }

                    log.info("Extracted from title page - Title: {}, Subtitle: {}",
                            metadata.getTitle(), metadata.getSubtitle());
                    return;
                }
            }

            // Fallback to dc:title if title page not found
            if (metadata.getTitle() == null) {
                String fullTitle = book.getMetadata().getTitles().isEmpty()
                        ? null : book.getMetadata().getTitles().get(0);

                if (fullTitle != null) {
                    // Try to split by common separators
                    if (fullTitle.contains(":")) {
                        String[] parts = fullTitle.split(":", 2);
                        metadata.setTitle(parts[0].trim());
                        if (parts.length > 1) {
                            metadata.setSubtitle(parts[1].trim());
                        }
                    } else {
                        metadata.setTitle(fullTitle);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract title/subtitle: {}", e.getMessage());
            // Fallback to dc:title
            String fullTitle = book.getMetadata().getTitles().isEmpty()
                    ? null : book.getMetadata().getTitles().get(0);
            metadata.setTitle(fullTitle);
        }
    }

    /**
     * ✅ FIX 2: Extract dcterms:modified from content.opf
     */
    private static LocalDateTime extractModifiedDate(Book book) {
        try {
            // Get content.opf as XML
            byte[] opfData = book.getOpfResource().getData();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(opfData));

            // Look for <meta property="dcterms:modified">
            NodeList metaNodes = doc.getElementsByTagName("meta");
            for (int i = 0; i < metaNodes.getLength(); i++) {
                Element meta = (Element) metaNodes.item(i);
                String property = meta.getAttribute("property");

                if ("dcterms:modified".equals(property)) {
                    String dateStr = meta.getTextContent().trim();
                    return parseIso8601DateTime(dateStr);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract dcterms:modified: {}", e.getMessage());
        }

        return null; // Return null if not found
    }

    /**
     * ✅ Extract dc:source from content.opf
     */
    private static String extractSource(Book book) {
        try {
            // Get content.opf as XML
            byte[] opfData = book.getOpfResource().getData();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(opfData));

            // Look for <dc:source> element
            NodeList sourceNodes = doc.getElementsByTagNameNS("http://purl.org/dc/elements/1.1/", "source");
            if (sourceNodes.getLength() > 0) {
                Element sourceElement = (Element) sourceNodes.item(0);
                String sourceText = sourceElement.getTextContent().trim();

                // Return if not empty
                if (sourceText != null && !sourceText.isEmpty()) {
                    return sourceText;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract dc:source: {}", e.getMessage());
        }

        return null;
    }

    /**
     * ✅ NEW: Extract contributors with roles from content.opf
     * epublib doesn't parse <meta refines> properly, so we need to do it manually
     */
    private static List<ContributorMetadata> extractContributorsWithRoles(Book book) {
        List<ContributorMetadata> contributors = new ArrayList<>();

        try {
            // Get content.opf as XML
            byte[] opfData = book.getOpfResource().getData();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(opfData));

            // Get all dc:contributor elements
            NodeList contributorNodes = doc.getElementsByTagNameNS("http://purl.org/dc/elements/1.1/", "contributor");

            for (int i = 0; i < contributorNodes.getLength(); i++) {
                Element contributorElement = (Element) contributorNodes.item(i);
                String contributorName = contributorElement.getTextContent().trim();
                String contributorId = contributorElement.getAttribute("id");

                if (contributorName.isEmpty()) {
                    continue;
                }

                // Find the role from <meta refines="#id" property="role">
                String role = "Contributor"; // Default

                if (contributorId != null && !contributorId.isEmpty()) {
                    NodeList metaNodes = doc.getElementsByTagName("meta");

                    for (int j = 0; j < metaNodes.getLength(); j++) {
                        Element meta = (Element) metaNodes.item(j);
                        String refines = meta.getAttribute("refines");
                        String property = meta.getAttribute("property");

                        // Check if this meta refines our contributor and has role property
                        if (("#" + contributorId).equals(refines) && "role".equals(property)) {
                            String roleCode = meta.getTextContent().trim();
                            role = mapRoleCode(roleCode);
                            log.info("✅ Found role for contributor '{}': code='{}', mapped='{}'",
                                    contributorName, roleCode, role);
                            break;
                        }
                    }
                }

                ContributorMetadata contribMeta = new ContributorMetadata();
                contribMeta.setName(contributorName);
                contribMeta.setRole(role);
                contributors.add(contribMeta);

                log.info("Extracted contributor: {} ({})", contributorName, role);
            }

        } catch (Exception e) {
            log.error("Failed to extract contributors from OPF: {}", e.getMessage());
            // Fallback to epublib's built-in extraction (without roles)
            return extractContributorsFallback(book.getMetadata());
        }

        return contributors;
    }

    /**
     * Map MARC relator codes to readable role names
     */
    private static String mapRoleCode(String code) {
        if (code == null || code.isEmpty()) {
            return "Contributor";
        }

        String trimmedCode = code.trim().toLowerCase();

        switch (trimmedCode) {
            case "trl": return "Translator";
            case "edt": return "Editor";
            case "ill": return "Illustrator";
            case "pbl": return "Publisher";
            case "aut": return "Author";
            case "pht": return "Photographer";
            case "cov": return "Cover Designer";
            case "ctb": return "Contributor";
            default:
                log.debug("Unknown relator code '{}', returning 'Contributor'", code);
                return "Contributor";
        }
    }

    /**
     * Fallback method using epublib's built-in extraction (without proper roles)
     */
    private static List<ContributorMetadata> extractContributorsFallback(Metadata metadata) {
        List<ContributorMetadata> contributors = new ArrayList<>();

        metadata.getContributors().forEach(contributor -> {
            ContributorMetadata contribMeta = new ContributorMetadata();

            String firstName = contributor.getFirstname();
            String lastName = contributor.getLastname();

            String name;
            if (firstName != null && lastName != null) {
                name = firstName + " " + lastName;
            } else if (firstName != null) {
                name = firstName;
            } else if (lastName != null) {
                name = lastName;
            } else {
                name = "Unknown Contributor";
            }

            contribMeta.setName(name);
            contribMeta.setRole(mapRole(contributor.getRelator()));
            contributors.add(contribMeta);
        });

        return contributors;
    }

    /**
     * Keep the old mapRole for compatibility with fallback method
     */
    private static String mapRole(nl.siegmann.epublib.domain.Relator relator) {
        if (relator == null) {
            log.debug("Relator is null, returning default 'Contributor'");
            return "Contributor";
        }

        String code = relator.getCode();
        log.debug("Mapping relator code: {}", code);

        if (code == null) {
            return "Contributor";
        }

        return mapRoleCode(code);
    }

    /**
     * Parse ISO 8601 datetime (e.g., "2025-12-17T14:51:26Z")
     */
    private static LocalDateTime parseIso8601DateTime(String dateStr) {
        try {
            // Remove 'Z' and parse
            String cleaned = dateStr.replace("Z", "");
            return LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse ISO 8601 date: {}", dateStr);
            return null;
        }
    }

    private static LocalDate extractPublicationDate(Metadata metadata) {
        if (metadata.getDates().isEmpty()) {
            return null;
        }

        try {
            String dateStr = metadata.getDates().get(0).getValue();

            // Try different date formats
            List<DateTimeFormatter> formatters = Arrays.asList(
                    DateTimeFormatter.ISO_LOCAL_DATE,
                    DateTimeFormatter.ofPattern("yyyy-MM"),
                    DateTimeFormatter.ofPattern("yyyy")
            );

            for (DateTimeFormatter formatter : formatters) {
                try {
                    if (dateStr.length() == 4) {
                        return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
                    } else if (dateStr.length() == 7) {
                        return LocalDate.parse(dateStr + "-01", DateTimeFormatter.ISO_LOCAL_DATE);
                    } else {
                        return LocalDate.parse(dateStr, formatter);
                    }
                } catch (DateTimeParseException ignored) {
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse publication date: {}", e.getMessage());
        }

        return null;
    }

    private static String parseCopyrightStatus(String rights) {
        if (rights == null) return "UNKNOWN";

        String lower = rights.toLowerCase();
        if (lower.contains("public domain") || lower.contains("cc0")) {
            return "PUBLIC_DOMAIN";
        } else if (lower.contains("creative commons") || lower.contains("cc by")) {
            return "CREATIVE_COMMONS";
        } else if (lower.contains("copyright")) {
            return "COPYRIGHTED";
        }

        return "UNKNOWN";
    }
}