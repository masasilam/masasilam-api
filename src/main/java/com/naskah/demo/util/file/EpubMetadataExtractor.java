package com.naskah.demo.util.file;

import com.naskah.demo.model.dto.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.EpubReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@UtilityClass
public class EpubMetadataExtractor {
    private static final String CONTRIBUTOR = "Contributor";

    public static CompleteEpubMetadata extractCompleteMetadata(InputStream epubInputStream) throws IOException {
        CompleteEpubMetadata metadata = new CompleteEpubMetadata();

        try {
            EpubReader reader = new EpubReader();
            Book book = reader.readEpub(epubInputStream);
            Metadata epubMetadata = book.getMetadata();

            extractTitleAndSubtitle(book, metadata);

            List<AuthorMetadata> authors = new ArrayList<>();
            epubMetadata.getAuthors().forEach(author -> {
                AuthorMetadata authorMeta = new AuthorMetadata();
                String firstName = author.getFirstname();
                String lastName = author.getLastname();

                authorMeta.setName(firstName + " " + lastName);
                authorMeta.setRole("Author");
                authors.add(authorMeta);
            });
            metadata.setAuthors(authors);

            List<ContributorMetadata> contributors = extractContributorsWithRoles(book);
            metadata.setContributors(contributors);
            metadata.setPublisher(epubMetadata.getPublishers().isEmpty() ? null : epubMetadata.getPublishers().getFirst());
            metadata.setLanguage(epubMetadata.getLanguage());
            metadata.setDescription(epubMetadata.getDescriptions().isEmpty() ? null : epubMetadata.getDescriptions().getFirst());
            metadata.setUpdatedAt(extractModifiedDate(book));

            LocalDate pubDate = extractPublicationDate(epubMetadata);
            metadata.setPublishedAt(pubDate);
            metadata.setPublicationYear(Objects.requireNonNull(pubDate).getYear());

            List<String> subjects = new ArrayList<>();
            if (epubMetadata.getSubjects() != null && !epubMetadata.getSubjects().isEmpty()) {
                for (String subject : epubMetadata.getSubjects()) {
                    if (subject != null && !subject.isEmpty()) {
                        subjects.add(subject);
                    }
                }
            }
            metadata.setSubjects(subjects);
            metadata.setCopyrightStatus(parseCopyrightStatus(epubMetadata.getRights().isEmpty() ? null : epubMetadata.getRights().getFirst()));
            metadata.setSource(extractSource(book));
            metadata.setCoverImageData(book.getCoverImage().getData());
            metadata.setCategory(subjects.getFirst());
        } catch (Exception e) {
            log.error("Failed to extract EPUB metadata: {}", e.getMessage(), e);
            throw e;
        }

        return metadata;
    }

    private static void extractTitleAndSubtitle(Book book, CompleteEpubMetadata metadata) {
        try {
            boolean extractedFromTitlePage = extractTitlePageContent(book, metadata);

            if (!extractedFromTitlePage) {
                extractTitleFromMetadata(book, metadata);
            }
        } catch (Exception e) {
            log.error("Failed to extract title/subtitle: {}", e.getMessage());
            String fullTitle = book.getMetadata().getTitles().isEmpty() ? null : book.getMetadata().getTitles().getFirst();
            metadata.setTitle(toTitleCase(fullTitle));
        }
    }

    private static boolean extractTitlePageContent(Book book, CompleteEpubMetadata metadata) throws IOException {
        for (var ref : book.getSpine().getSpineReferences()) {
            var resource = ref.getResource();
            String content = new String(resource.getData(), StandardCharsets.UTF_8);

            boolean isTitlePage = content.contains("titlepage") || content.contains("epub:type=\"titlepage\"");
            if (!isTitlePage) {
                continue;
            }

            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(content);
            org.jsoup.nodes.Element h1 = doc.select("h1").first();
            metadata.setTitle(toTitleCase(Objects.requireNonNull(h1).text().trim()));

            org.jsoup.nodes.Element h2 = doc.select("h2.subtitle, h2.sigil_not_in_toc").first();
            metadata.setSubtitle(toTitleCase(Objects.requireNonNull(h2).text().trim()));

            log.info("Extracted from title page - Title: {}, Subtitle: {}", metadata.getTitle(), metadata.getSubtitle());
            return true;
        }
        return false;
    }

    private static void extractTitleFromMetadata(Book book, CompleteEpubMetadata metadata) {
        if (metadata.getTitle() != null) {
            return;
        }

        String fullTitle = book.getMetadata().getTitles().isEmpty() ? null : book.getMetadata().getTitles().getFirst();
        if (fullTitle == null) {
            return;
        }

        if (fullTitle.contains(":")) {
            String[] parts = fullTitle.split(":", 2);
            metadata.setTitle(toTitleCase(parts[0].trim()));
            if (parts.length > 1) {
                metadata.setSubtitle(toTitleCase(parts[1].trim()));
            }
        } else {
            metadata.setTitle(toTitleCase(fullTitle));
        }
    }

    private static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Kata-kata yang tetap lowercase kecuali di awal kalimat
        List<String> lowercaseWords = Arrays.asList(
                // kata depan
                "di", "ke", "dari", "tentang",
                // kata hubung
                "dan", "atau", "karena", "yang",
                // kata seru
                "oh", "dong", "kok", "sih",
                // kata sandang
                "si", "sang",
                // partikel
                "pun", "per"
        );

        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }

            String word = words[i];

            // Kata pertama selalu dikapitalisasi
            if (i == 0) {
                result.append(capitalizeFirstLetter(word));
            }
            // Kata dalam daftar lowercase tetap lowercase (kecuali kata pertama)
            else if (lowercaseWords.contains(word)) {
                result.append(word);
            }
            // Kata lainnya dikapitalisasi
            else {
                result.append(capitalizeFirstLetter(word));
            }
        }

        return result.toString();
    }

    private static String capitalizeFirstLetter(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    private static LocalDateTime extractModifiedDate(Book book) {
        try {
            byte[] opfData = book.getOpfResource().getData();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(opfData));

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

        return null;
    }

    private static String extractSource(Book book) {
        try {
            byte[] opfData = book.getOpfResource().getData();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(opfData));

            NodeList sourceNodes = doc.getElementsByTagNameNS("http://purl.org/dc/elements/1.1/", "source");
            if (sourceNodes.getLength() > 0) {
                Element sourceElement = (Element) sourceNodes.item(0);
                String sourceText = sourceElement.getTextContent().trim();

                if (!sourceText.isEmpty()) {
                    return sourceText;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract dc:source: {}", e.getMessage());
        }

        return null;
    }

    private static List<ContributorMetadata> extractContributorsWithRoles(Book book) {
        List<ContributorMetadata> contributors = new ArrayList<>();

        try {
            byte[] opfData = book.getOpfResource().getData();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(opfData));

            NodeList contributorNodes = doc.getElementsByTagNameNS("http://purl.org/dc/elements/1.1/", "contributor");

            log.info("Found {} dc:contributor nodes in OPF", contributorNodes.getLength());

            for (int i = 0; i < contributorNodes.getLength(); i++) {
                Element contributorElement = (Element) contributorNodes.item(i);
                String contributorName = contributorElement.getTextContent().trim();
                String contributorId = contributorElement.getAttribute("id");

                log.info("Processing contributor node #{}: name='{}', id='{}'", i+1, contributorName, contributorId);

                if (contributorName.isEmpty()) {
                    log.warn("Contributor name is empty, skipping");
                    continue;
                }

                String role = "Contributor";

                if (!contributorId.isEmpty()) {
                    NodeList metaNodes = doc.getElementsByTagName("meta");
                    log.info("Searching for role in {} meta nodes for contributor id: {}", metaNodes.getLength(), contributorId);

                    for (int j = 0; j < metaNodes.getLength(); j++) {
                        Element meta = (Element) metaNodes.item(j);
                        String refines = meta.getAttribute("refines");
                        String property = meta.getAttribute("property");

                        if (("#" + contributorId).equals(refines) && "role".equals(property)) {
                            String roleCode = meta.getTextContent().trim();
                            role = mapRoleCode(roleCode);
                            log.info("Found role for contributor '{}': code='{}', mapped='{}'", contributorName, roleCode, role);
                            break;
                        }
                    }
                } else {
                    log.warn("Contributor '{}' has no id attribute, using default role", contributorName);
                }

                ContributorMetadata contribMeta = new ContributorMetadata();
                contribMeta.setName(contributorName);
                contribMeta.setRole(role);
                contributors.add(contribMeta);

                log.info("Successfully extracted contributor: {} ({})", contributorName, role);
            }

            if (contributors.isEmpty()) {
                log.warn("No contributors found in OPF, trying fallback method");
                return extractContributorsFallback(book.getMetadata());
            }

            log.info("Total contributors extracted from OPF: {}", contributors.size());

        } catch (Exception e) {
            log.error("Failed to extract contributors from OPF: {}", e.getMessage(), e);
            return extractContributorsFallback(book.getMetadata());
        }

        return contributors;
    }

    private static String mapRoleCode(String code) {
        if (code == null || code.isEmpty()) {
            return CONTRIBUTOR;
        }

        String trimmedCode = code.trim().toLowerCase();

        return switch (trimmedCode) {
            case "trl" -> "Translator";
            case "edt" -> "Editor";
            case "ill" -> "Illustrator";
            case "pbl" -> "Publisher";
            case "aut" -> "Author";
            case "pht" -> "Photographer";
            case "cov" -> "Cover Designer";
            case "ctb" -> CONTRIBUTOR;
            default -> {
                log.debug("Unknown relator code '{}', returning 'Contributor'", code);
                yield CONTRIBUTOR;
            }
        };
    }

    private static List<ContributorMetadata> extractContributorsFallback(Metadata metadata) {
        List<ContributorMetadata> contributors = new ArrayList<>();

        metadata.getContributors().forEach(contributor -> {
            ContributorMetadata contribMeta = new ContributorMetadata();

            String firstName = contributor.getFirstname();
            String lastName = contributor.getLastname();

            String name;
            if (firstName != null && lastName != null) {
                name = firstName + " " + lastName;
            } else
                name = Objects.requireNonNullElseGet(firstName, () -> Objects.requireNonNullElse(lastName, "Unknown Contributor"));

            contribMeta.setName(name);
            contribMeta.setRole(mapRole(contributor.getRelator()));
            contributors.add(contribMeta);
        });

        return contributors;
    }

    private static String mapRole(nl.siegmann.epublib.domain.Relator relator) {
        if (relator == null) {
            log.debug("Relator is null, returning default 'Contributor'");
            return CONTRIBUTOR;
        }

        String code = relator.getCode();
        log.debug("Mapping relator code: {}", code);

        if (code == null) {
            return CONTRIBUTOR;
        }

        return mapRoleCode(code);
    }

    private static LocalDateTime parseIso8601DateTime(String dateStr) {
        try {
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
            String dateStr = metadata.getDates().getFirst().getValue();

            List<DateTimeFormatter> formatters = Arrays.asList(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("yyyy-MM"), DateTimeFormatter.ofPattern("yyyy"));

            for (DateTimeFormatter formatter : formatters) {
                if (dateStr.length() == 4) {
                    return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
                } else if (dateStr.length() == 7) {
                    return LocalDate.parse(dateStr + "-01", DateTimeFormatter.ISO_LOCAL_DATE);
                } else {
                    return LocalDate.parse(dateStr, formatter);
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