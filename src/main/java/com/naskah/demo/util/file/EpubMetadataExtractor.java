package com.naskah.demo.util.file;

import com.naskah.demo.model.dto.AuthorMetadata;
import com.naskah.demo.model.dto.CompleteEpubMetadata;
import com.naskah.demo.model.dto.ContributorMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class EpubMetadataExtractor {

    private static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";

    /**
     * Extract complete metadata dari EPUB file
     */
    public static CompleteEpubMetadata extractCompleteMetadata(InputStream epubInputStream) throws Exception {
        log.info("Starting complete EPUB metadata extraction");

        byte[] epubBytes = epubInputStream.readAllBytes();

        // Extract metadata dari content.opf
        byte[] opfContent = findAndReadContentOpf(new ByteArrayInputStream(epubBytes));
        if (opfContent == null) {
            throw new Exception("content.opf not found in EPUB file");
        }

        CompleteEpubMetadata metadata = parseContentOpf(opfContent);

        // Extract cover image
        String coverPath = metadata.getCoverPath();
        if (coverPath != null) {
            byte[] coverData = extractFileFromEpub(new ByteArrayInputStream(epubBytes), coverPath);
            metadata.setCoverImageData(coverData);
        }

        return metadata;
    }

    /**
     * Mencari dan membaca file content.opf
     */
    private static byte[] findAndReadContentOpf(InputStream epubInputStream) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(epubInputStream)) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.endsWith("content.opf") || entryName.endsWith("package.opf")) {
                    log.info("Found OPF file: {}", entryName);
                    return zis.readAllBytes();
                }
            }
        }

        return null;
    }

    /**
     * Extract specific file dari EPUB
     */
    private static byte[] extractFileFromEpub(InputStream epubInputStream, String targetPath) {
        try (ZipInputStream zis = new ZipInputStream(epubInputStream)) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.equals(targetPath) || entryName.endsWith(targetPath)) {
                    log.info("Found cover image: {}", entryName);
                    return zis.readAllBytes();
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract file from EPUB: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Parse content.opf XML
     */
    private static CompleteEpubMetadata parseContentOpf(byte[] opfContent) throws Exception {
        CompleteEpubMetadata metadata = new CompleteEpubMetadata();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(opfContent));

        Element root = doc.getDocumentElement();
        NodeList metadataNodes = root.getElementsByTagName("metadata");

        if (metadataNodes.getLength() == 0) {
            throw new Exception("No metadata section found");
        }

        Element metadataElement = (Element) metadataNodes.item(0);

        // Extract all metadata
        metadata.setTitle(getTextContent(metadataElement, "title"));
        metadata.setSubtitle(getMetaProperty(metadataElement, "title-type", "subtitle"));
        metadata.setDescription(getTextContent(metadataElement, "description"));
        metadata.setPublisher(getTextContent(metadataElement, "publisher"));
        metadata.setLanguage(getTextContent(metadataElement, "language"));
        metadata.setRights(getTextContent(metadataElement, "rights"));
        metadata.setSource(getTextContent(metadataElement, "source"));
        metadata.setIdentifier(getTextContent(metadataElement, "identifier"));

        // Extract and parse date
        String dateStr = getTextContent(metadataElement, "date");
        metadata.setPublicationYear(extractYear(dateStr));
        metadata.setPublishedAt(parseDate(dateStr));

        // Extract subjects
        metadata.setSubjects(getMultipleTextContent(metadataElement, "subject"));

        // Extract authors
        metadata.setAuthors(extractCreators(metadataElement));

        // Extract contributors
        metadata.setContributors(extractContributors(metadataElement));

        // Extract cover path dari manifest
        String coverId = getCoverMetaContent(metadataElement);
        if (coverId != null) {
            String coverPath = findCoverPath(root, coverId);
            metadata.setCoverPath(coverPath);
        }

        // Auto-detect category dan copyright
        metadata.setCategory(detectCategory(metadata.getSubjects()));
        metadata.setCopyrightStatus(detectCopyrightStatus(metadata.getRights()));

        log.info("Extracted complete metadata: title={}, authors={}, cover={}",
                metadata.getTitle(), metadata.getAuthors().size(), metadata.getCoverPath());

        return metadata;
    }

    private static String getCoverMetaContent(Element metadataElement) {
        NodeList metaNodes = metadataElement.getElementsByTagName("meta");

        for (int i = 0; i < metaNodes.getLength(); i++) {
            Element meta = (Element) metaNodes.item(i);
            String name = meta.getAttribute("name");

            if ("cover".equals(name)) {
                return meta.getAttribute("content");
            }
        }

        return null;
    }

    private static String findCoverPath(Element root, String coverId) {
        NodeList manifestNodes = root.getElementsByTagName("manifest");

        if (manifestNodes.getLength() > 0) {
            Element manifest = (Element) manifestNodes.item(0);
            NodeList items = manifest.getElementsByTagName("item");

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String id = item.getAttribute("id");

                if (coverId.equals(id)) {
                    String href = item.getAttribute("href");
                    log.info("Found cover path: {}", href);
                    return href;
                }
            }
        }

        return null;
    }

    private static String detectCategory(List<String> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return "General";
        }

        String subjectsLower = subjects.toString().toLowerCase();

        if (subjectsLower.contains("puisi") || subjectsLower.contains("poetry") ||
                subjectsLower.contains("poem")) {
            return "Poetry";
        }

        if (subjectsLower.contains("novel") || subjectsLower.contains("fiction") ||
                subjectsLower.contains("cerita")) {
            return "Fiction";
        }

        if (subjectsLower.contains("sejarah") || subjectsLower.contains("history") ||
                subjectsLower.contains("biography") || subjectsLower.contains("biografi")) {
            return "Non-Fiction";
        }

        if (subjectsLower.contains("filsafat") || subjectsLower.contains("philosophy")) {
            return "Philosophy";
        }

        if (subjectsLower.contains("sains") || subjectsLower.contains("science")) {
            return "Science";
        }

        return "Literature";
    }

    private static String detectCopyrightStatus(String rights) {
        if (rights == null || rights.isEmpty()) {
            return "UNKNOWN";
        }

        String rightsLower = rights.toLowerCase();

        if (rightsLower.contains("public domain") || rightsLower.contains("domain publik") ||
                rightsLower.contains("cc0") || rightsLower.contains("creative commons zero")) {
            return "PUBLIC_DOMAIN";
        }

        if (rightsLower.contains("creative commons") || rightsLower.contains("cc by")) {
            return "CREATIVE_COMMONS";
        }

        if (rightsLower.contains("copyright") || rightsLower.contains("all rights reserved")) {
            return "COPYRIGHTED";
        }

        return "UNKNOWN";
    }

    private static List<AuthorMetadata> extractCreators(Element metadataElement) {
        List<AuthorMetadata> authors = new ArrayList<>();
        NodeList creatorNodes = metadataElement.getElementsByTagNameNS(DC_NAMESPACE, "creator");

        for (int i = 0; i < creatorNodes.getLength(); i++) {
            Element creatorElement = (Element) creatorNodes.item(i);
            String name = creatorElement.getTextContent().trim();
            String id = creatorElement.getAttribute("id");

            if (!name.isEmpty()) {
                AuthorMetadata author = new AuthorMetadata();
                author.setName(name);

                if (!id.isEmpty()) {
                    String fileAs = getMetaRefines(metadataElement, id, "file-as");
                    if (fileAs != null) {
                        author.setFileAs(fileAs);
                    }

                    String role = getMetaRefines(metadataElement, id, "role");
                    if (role != null) {
                        author.setRole(role);
                    }
                }

                authors.add(author);
            }
        }

        return authors;
    }

    private static List<ContributorMetadata> extractContributors(Element metadataElement) {
        List<ContributorMetadata> contributors = new ArrayList<>();
        NodeList contributorNodes = metadataElement.getElementsByTagNameNS(DC_NAMESPACE, "contributor");

        for (int i = 0; i < contributorNodes.getLength(); i++) {
            Element contributorElement = (Element) contributorNodes.item(i);
            String name = contributorElement.getTextContent().trim();
            String id = contributorElement.getAttribute("id");

            if (!name.isEmpty()) {
                ContributorMetadata contributor = new ContributorMetadata();
                contributor.setName(name);

                if (!id.isEmpty()) {
                    String role = getMetaRefines(metadataElement, id, "role");
                    if (role != null) {
                        contributor.setRole(mapMarcRole(role));
                    }
                }

                contributors.add(contributor);
            }
        }

        return contributors;
    }

    private static String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagNameNS(DC_NAMESPACE, tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    private static List<String> getMultipleTextContent(Element parent, String tagName) {
        List<String> results = new ArrayList<>();
        NodeList nodes = parent.getElementsByTagNameNS(DC_NAMESPACE, tagName);

        for (int i = 0; i < nodes.getLength(); i++) {
            String text = nodes.item(i).getTextContent().trim();
            if (!text.isEmpty()) {
                results.add(text);
            }
        }

        return results;
    }

    private static String getMetaProperty(Element parent, String property, String value) {
        NodeList metaNodes = parent.getElementsByTagName("meta");

        for (int i = 0; i < metaNodes.getLength(); i++) {
            Element meta = (Element) metaNodes.item(i);
            if (value.equals(meta.getAttribute(property))) {
                return meta.getTextContent().trim();
            }
        }

        return null;
    }

    private static String getMetaRefines(Element parent, String refId, String property) {
        NodeList metaNodes = parent.getElementsByTagName("meta");

        for (int i = 0; i < metaNodes.getLength(); i++) {
            Element meta = (Element) metaNodes.item(i);
            String refines = meta.getAttribute("refines");
            String prop = meta.getAttribute("property");

            if (refines.equals("#" + refId) && prop.contains(property)) {
                return meta.getTextContent().trim();
            }
        }

        return null;
    }

    private static Integer extractYear(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            String yearStr = dateStr.substring(0, Math.min(4, dateStr.length()));
            return Integer.parseInt(yearStr);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            if (dateStr.length() == 4) {
                return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
            } else if (dateStr.length() == 7) {
                return LocalDate.parse(dateStr + "-01", DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String mapMarcRole(String marcCode) {
        return switch (marcCode.toLowerCase()) {
            case "aut" -> "Author";
            case "ill", "art" -> "Illustrator";
            case "edt" -> "Editor";
            case "trl" -> "Translator";
            case "pbl" -> "Publisher";
            case "pht" -> "Photographer";
            case "dsr" -> "Designer";
            case "ctb" -> "Contributor";
            default -> marcCode;
        };
    }
}