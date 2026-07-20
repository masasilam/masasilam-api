package com.naskah.app.util.file;

import com.naskah.app.model.dto.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.EpubReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@UtilityClass
public class EpubMetadataExtractor {
    private static final String PROPERTY = "property";
    private static final String REFINES = "refines";
    private static final String CONTRIBUTOR = "Contributor";
    private static final String AUTHOR = "Author";
    private static final String  HTTPPURL = "http://purl.org/dc/elements/1.1/";
    
    public static CompleteEpubMetadata extractCompleteMetadata(InputStream epubInputStream) throws Exception {
        CompleteEpubMetadata metadata = new CompleteEpubMetadata();
        try {
            EpubReader reader = new EpubReader();
            Book book = reader.readEpub(epubInputStream);
            Metadata epubMetadata = book.getMetadata();

            Document opfDoc = parseOpfDocument(book);

            extractTitleAndSubtitle(book, metadata);

            metadata.setAuthors(extractAuthorsWithMetadata(book, opfDoc));
            metadata.setContributors(extractContributorsWithRoles(book, opfDoc));

            metadata.setPublisher(epubMetadata.getPublishers().isEmpty() ? null : epubMetadata.getPublishers().getFirst());
            metadata.setLanguage(epubMetadata.getLanguage());
            metadata.setDescription(epubMetadata.getDescriptions().isEmpty() ? null : epubMetadata.getDescriptions().getFirst());
            metadata.setUpdatedAt(extractModifiedDate(opfDoc));

            LocalDate pubDate = extractPublicationDate(epubMetadata);
            metadata.setPublishedAt(pubDate);
            metadata.setPublicationYear(pubDate != null ? pubDate.getYear() : null);

            List<String> subjects = new ArrayList<>();
            if (epubMetadata.getSubjects() != null) {
                for (String subject : epubMetadata.getSubjects()) {
                    if (subject != null && !subject.isEmpty()) subjects.add(subject);
                }
            }
            metadata.setSubjects(subjects);
            metadata.setCategory(subjects.isEmpty() ? null : subjects.getFirst());

            metadata.setCopyrightStatus(parseCopyrightStatus(epubMetadata.getRights().isEmpty() ? null : epubMetadata.getRights().getFirst()));
            metadata.setSource(extractSource(opfDoc));
            metadata.setCoverImageData(book.getCoverImage().getData());

            extractSeriesMetadata(opfDoc, metadata);
            extractFirstPublicationMetadata(opfDoc, metadata);

        } catch (Exception e) {
            log.error("Failed to extract EPUB metadata: {}", e.getMessage(), e);
            throw e;
        }

        return metadata;
    }

    private static Document parseOpfDocument(Book book) throws IOException, ParserConfigurationException, SAXException {
        byte[] opfData = book.getOpfResource().getData();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception e) {
            log.warn("Could not set XML security features: {}", e.getMessage());
        }

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(opfData));
    }

    private static void extractSeriesMetadata(Document doc, CompleteEpubMetadata metadata) {
        try {
            NodeList metaNodes = doc.getElementsByTagName("meta");

            Map<String, String> collectionNames = new LinkedHashMap<>();
            Map<String, String> collectionTypes = new LinkedHashMap<>();
            Map<String, String> collectionPosition = new LinkedHashMap<>();
            Map<String, String> collectionDesc = new LinkedHashMap<>();

            for (int i = 0; i < metaNodes.getLength(); i++) {
                Element meta = (Element) metaNodes.item(i);
                String property = meta.getAttribute(PROPERTY);
                String refines = meta.getAttribute(REFINES);
                String id = meta.getAttribute("id");
                String value = meta.getTextContent().trim();

                if ("belongs-to-collection".equals(property) && !id.isEmpty()) {
                    collectionNames.put(id, value);
                }
                if (refines.startsWith("#") && "collection-type".equals(property)) {
                    collectionTypes.put(refines.substring(1), value);
                }
                if (refines.startsWith("#") && "group-position".equals(property)) {
                    collectionPosition.put(refines.substring(1), value);
                }
                if (refines.startsWith("#") && "schema:description".equals(property)) {
                    collectionDesc.put(refines.substring(1), value);
                }
            }

            for (Map.Entry<String, String> entry : collectionNames.entrySet()) {
                String colId = entry.getKey();
                String colType = collectionTypes.getOrDefault(colId, "");
                String pos = collectionPosition.get(colId);

                metadata.setCollectionName(entry.getValue());
                metadata.setCollectionType(colType);

                if ("series".equals(colType)) {
                    metadata.setSeriesName(entry.getValue());
                    metadata.setSeriesDescription(collectionDesc.get(colId));
                    if (pos != null) {
                        metadata.setSeriesOrder(Integer.parseInt(pos));
                    }
                    log.info("Extracted series: '{}', order: {}", metadata.getSeriesName(), metadata.getSeriesOrder());
                } else if ("periodical".equals(colType) && pos != null) {
                    metadata.setIssueNumber(Integer.parseInt(pos));
                    log.info("Extracted periodical: '{}', issue: {}", entry.getValue(), metadata.getIssueNumber());
                }
            }

        } catch (Exception e) {
            log.warn("Failed to extract series/periodical metadata: {}", e.getMessage());
        }
    }

    private static void extractFirstPublicationMetadata(Document doc, CompleteEpubMetadata metadata) {
        try {
            NodeList metaNodes = doc.getElementsByTagName("meta");

            for (int i = 0; i < metaNodes.getLength(); i++) {
                Element meta = (Element) metaNodes.item(i);
                String property = meta.getAttribute(PROPERTY);
                String value = meta.getTextContent().trim();

                if (value.isEmpty()) continue;

                if ("schema:firstPublished".equals(property)) {
                    metadata.setFirstPublished(value);
                    log.info("Extracted firstPublished: {}", value);
                }
                if ("schema:firstPublisher".equals(property)) {
                    metadata.setFirstPublisher(value);
                    log.info("Extracted firstPublisher: {}", value);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to extract first publication metadata: {}", e.getMessage());
        }
    }

    private static List<AuthorMetadata> extractAuthorsWithMetadata(Book book, Document doc) {
        List<AuthorMetadata> authors = new ArrayList<>();
        try {
            NodeList creatorNodes = doc.getElementsByTagNameNS(HTTPPURL, "creator");
            log.info("Found {} dc:creator nodes in OPF", creatorNodes.getLength());

            for (int i = 0; i < creatorNodes.getLength(); i++) {
                Element creatorElement = (Element) creatorNodes.item(i);
                String authorName = creatorElement.getTextContent().trim();
                String creatorId = creatorElement.getAttribute("id");

                log.info("Processing author node #{}: name='{}', id='{}'", i + 1, authorName, creatorId);

                if (authorName.isEmpty()) {
                    log.warn("Author name is empty, skipping");
                    continue;
                }

                AuthorMetadata authorMeta = new AuthorMetadata();
                authorMeta.setName(authorName);
                authorMeta.setRole(AUTHOR);

                if (!creatorId.isEmpty()) {
                    extractSchemaMetadataByRefines(doc, creatorId, authorMeta);
                    extractTermMetadata(doc, creatorId, authorMeta);
                }

                authors.add(authorMeta);
                log.info("Successfully extracted author: {} with metadata", authorName);
            }

            if (authors.isEmpty()) {
                log.warn("No authors found in OPF, using fallback");
                return extractAuthorsFallback(book.getMetadata());
            }

        } catch (Exception e) {
            log.error("Failed to extract authors from OPF: {}", e.getMessage(), e);
            return extractAuthorsFallback(book.getMetadata());
        }

        return authors;
    }

    private static void extractSchemaMetadataByRefines(Document doc, String targetId, AuthorMetadata authorMeta) {
        NodeList metaNodes = doc.getElementsByTagName("meta");

        for (int j = 0; j < metaNodes.getLength(); j++) {
            Element meta = (Element) metaNodes.item(j);
            String refines = meta.getAttribute(REFINES);
            String property = meta.getAttribute(PROPERTY);
            String content = meta.getTextContent().trim();

            if (!("#" + targetId).equals(refines) || content.isEmpty()) continue;

            switch (property) {
                case "schema:birthDate" -> {
                    try {
                        authorMeta.setBirthDate(String.valueOf(LocalDate.parse(content)));
                    } catch (Exception e) {
                        log.warn("Failed to parse birthDate: {}", content);
                    }
                }
                case "schema:deathDate" -> {
                    try {
                        authorMeta.setDeathDate(String.valueOf(LocalDate.parse(content)));
                    } catch (Exception e) {
                        log.warn("Failed to parse deathDate: {}", content);
                    }
                }
                case "schema:birthPlace" -> authorMeta.setBirthPlace(content);
                case "schema:deathPlace" -> authorMeta.setDeathPlace(content);
                case "schema:nationality" -> authorMeta.setNationality(content);
                case "schema:description" -> authorMeta.setBiography(content);
                case "schema:image" -> authorMeta.setPhotoUrl(content);
                default -> log.debug("Unhandled schema property for refines {}: {}", targetId, property);
            }
        }
    }

    private static void extractTermMetadata(Document doc, String creatorId, AuthorMetadata authorMeta) {
        NodeList metaNodes = doc.getElementsByTagName("meta");

        for (int j = 0; j < metaNodes.getLength(); j++) {
            Element meta = (Element) metaNodes.item(j);
            String refines = meta.getAttribute(REFINES);
            String property = meta.getAttribute(PROPERTY);

            if (("#" + creatorId).equals(refines) && "term".equals(property)) {
                String term = meta.getTextContent().trim();
                log.info("Found term metadata: {}", term);

                if (term.contains("-") && term.length() >= 9) {
                    String[] years = term.split("-");
                    try {
                        if (authorMeta.getBirthDate() == null) {
                            int birthYear = Integer.parseInt(years[0].trim());
                            authorMeta.setBirthDate(String.valueOf(LocalDate.of(birthYear, 1, 1)));
                            log.info("Extracted birthYear from term: {}", birthYear);
                        }
                        if (years.length > 1 && authorMeta.getDeathDate() == null) {
                            int deathYear = Integer.parseInt(years[1].trim());
                            authorMeta.setDeathDate(String.valueOf(LocalDate.of(deathYear, 1, 1)));
                            log.info("Extracted deathYear from term: {}", deathYear);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse term years: {}", term);
                    }
                }
                break;
            }
        }
    }

    private static List<AuthorMetadata> extractAuthorsFallback(Metadata metadata) {
        List<AuthorMetadata> authors = new ArrayList<>();
        metadata.getAuthors().forEach(author -> {
            AuthorMetadata authorMeta = new AuthorMetadata();
            authorMeta.setName(author.getFirstname() + " " + author.getLastname());
            authorMeta.setRole(AUTHOR);
            authors.add(authorMeta);
        });
        log.info("Extracted {} authors using fallback method", authors.size());
        return authors;
    }

    private static List<ContributorMetadata> extractContributorsWithRoles(Book book, Document doc) {
        List<ContributorMetadata> contributors = new ArrayList<>();

        try {
            NodeList contributorNodes = doc.getElementsByTagNameNS(HTTPPURL, "contributor");
            log.info("Found {} dc:contributor nodes in OPF", contributorNodes.getLength());

            for (int i = 0; i < contributorNodes.getLength(); i++) {
                Element contributorElement = (Element) contributorNodes.item(i);
                String contributorName = contributorElement.getTextContent().trim();
                String contributorId = contributorElement.getAttribute("id");

                log.info("Processing contributor node #{}: name='{}', id='{}'", i + 1, contributorName, contributorId);

                if (contributorName.isEmpty()) {
                    log.warn("Contributor name is empty, skipping");
                    continue;
                }

                String role = CONTRIBUTOR;

                if (!contributorId.isEmpty()) {
                    NodeList metaNodes = doc.getElementsByTagName("meta");
                    for (int j = 0; j < metaNodes.getLength(); j++) {
                        Element meta = (Element) metaNodes.item(j);
                        String refines = meta.getAttribute(REFINES);
                        String property = meta.getAttribute(PROPERTY);

                        if (("#" + contributorId).equals(refines) && "role".equals(property)) {
                            role = mapRoleCode(meta.getTextContent().trim());
                            log.info("Found role for contributor '{}': '{}'", contributorName, role);
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
                log.warn("No contributors found in OPF, trying fallback");
                return extractContributorsFallback(book.getMetadata());
            }

        } catch (Exception e) {
            log.error("Failed to extract contributors from OPF: {}", e.getMessage(), e);
            return extractContributorsFallback(book.getMetadata());
        }

        return contributors;
    }

    private static List<ContributorMetadata> extractContributorsFallback(Metadata metadata) {
        List<ContributorMetadata> contributors = new ArrayList<>();
        metadata.getContributors().forEach(contributor -> {
            ContributorMetadata contribMeta = new ContributorMetadata();
            String firstName = contributor.getFirstname();
            String lastName = contributor.getLastname();
            String name = (firstName != null && lastName != null)
                    ? firstName + " " + lastName
                    : Objects.requireNonNullElseGet(firstName,
                    () -> Objects.requireNonNullElse(lastName, "Unknown Contributor"));
            contribMeta.setName(name);
            contribMeta.setRole(mapRole(contributor.getRelator()));
            contributors.add(contribMeta);
        });
        return contributors;
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
            if (!isTitlePage) continue;

            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(content);
            org.jsoup.nodes.Element h1 = doc.select("h1").first();
            if (h1 == null) continue;
            metadata.setTitle(toTitleCase(h1.text().trim()));

            org.jsoup.nodes.Element h2 = doc.select("h2.subtitle, h2.sigil_not_in_toc").first();
            if (h2 != null) metadata.setSubtitle(toTitleCase(h2.text().trim()));

            log.info("Extracted from title page - Title: {}, Subtitle: {}", metadata.getTitle(), metadata.getSubtitle());
            return true;
        }
        return false;
    }

    private static void extractTitleFromMetadata(Book book, CompleteEpubMetadata metadata) {
        if (metadata.getTitle() != null) return;

        String fullTitle = book.getMetadata().getTitles().isEmpty()
                ? null : book.getMetadata().getTitles().getFirst();
        if (fullTitle == null) return;

        if (fullTitle.contains(":")) {
            String[] parts = fullTitle.split(":", 2);
            metadata.setTitle(toTitleCase(parts[0].trim()));
            if (parts.length > 1) metadata.setSubtitle(toTitleCase(parts[1].trim()));
        } else {
            metadata.setTitle(toTitleCase(fullTitle));
        }
    }

    private static LocalDateTime extractModifiedDate(Document doc) {
        try {
            NodeList metaNodes = doc.getElementsByTagName("meta");
            for (int i = 0; i < metaNodes.getLength(); i++) {
                Element meta = (Element) metaNodes.item(i);
                if ("dcterms:modified".equals(meta.getAttribute(PROPERTY))) {
                    return parseIso8601DateTime(meta.getTextContent().trim());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract dcterms:modified: {}", e.getMessage());
        }
        return null;
    }

    private static String extractSource(Document doc) {
        try {
            NodeList sourceNodes = doc.getElementsByTagNameNS(HTTPPURL, "source");
            if (sourceNodes.getLength() > 0) {
                String src = (sourceNodes.item(0)).getTextContent().trim();
                return src.isEmpty() ? null : src;
            }
        } catch (Exception e) {
            log.warn("Failed to extract dc:source: {}", e.getMessage());
        }
        return null;
    }

    private static LocalDate extractPublicationDate(Metadata metadata) {
        if (metadata.getDates().isEmpty()) return null;

        try {
            String dateStr = metadata.getDates().getFirst().getValue();
            if (dateStr.length() == 4) {
                return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
            } else if (dateStr.length() == 7) {
                return LocalDate.parse(dateStr + "-01", DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception e) {
            log.error("Failed to parse publication date: {}", e.getMessage());
        }
        return null;
    }

    private static String parseCopyrightStatus(String rights) {
        if (rights == null) return "UNKNOWN";
        String lower = rights.toLowerCase();
        if (lower.contains("public domain") || lower.contains("cc0")) return "PUBLIC_DOMAIN";
        if (lower.contains("creative commons") || lower.contains("cc by")) return "CREATIVE_COMMONS";
        if (lower.contains("copyright")) return "COPYRIGHTED";
        return "UNKNOWN";
    }

    private static String mapRoleCode(String code) {
        if (code == null || code.isEmpty()) return CONTRIBUTOR;
        return switch (code.trim().toLowerCase()) {
            case "trl" -> "Translator";
            case "trc" -> "Transcriber";
            case "edt" -> "Editor";
            case "ill" -> "Illustrator";
            case "pbl" -> "Publisher";
            case "aut" -> AUTHOR;
            case "pht" -> "Photographer";
            case "cov" -> "Cover Designer";
            case "art" -> "Cover Artist";
            case "ann" -> "Annotator";
            case "pfr" -> "Proofreader";
            case "dst" -> "Digital Specialist";
            case "ctb" -> CONTRIBUTOR;
            default -> {
                log.debug("Unknown relator code '{}', returning 'Contributor'", code);
                yield CONTRIBUTOR;
            }
        };
    }

    private static String mapRole(nl.siegmann.epublib.domain.Relator relator) {
        if (relator == null) return CONTRIBUTOR;
        String code = relator.getCode();
        return code == null ? CONTRIBUTOR : mapRoleCode(code);
    }

    private static LocalDateTime parseIso8601DateTime(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr.replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse ISO 8601 date: {}", dateStr);
            return null;
        }
    }

    private static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) return text;

        List<String> lowercaseWords = Arrays.asList(
                "di", "ke", "dari", "tentang", "oleh",
                "dan", "atau", "karena", "yang", "pada",
                "oh", "dong", "kok", "sih", "untuk",
                "si", "sang", "pun", "per", "dengan");

        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            String word = words[i];
            if (i == 0 || !lowercaseWords.contains(word)) {
                result.append(capitalizeFirstLetter(word));
            } else {
                result.append(word);
            }
        }
        return result.toString();
    }

    private static String capitalizeFirstLetter(String word) {
        if (word == null || word.isEmpty()) return word;
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }
}