package com.naskah.demo.util.file;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.naskah.demo.model.entity.ProjectPage;
import com.naskah.demo.model.enums.FileType;
import com.naskah.demo.model.enums.OCRStatus;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class FileUtil {
    private static Cloudinary cloudinary;
    private static final String storageRoot = Paths.get(System.getProperty("user.dir"), "storage").toString();
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "epub", "pdf", "doc", "docx");
    private static final List<String> OCR_SUPPORTED_EXTENSIONS = List.of("jpg", "jpeg", "png", "pdf");
    private static final int WORDS_PER_MINUTE = 200;

    public FileUtil(Cloudinary cloudinary) {
        FileUtil.cloudinary = cloudinary;
        try {
            Files.createDirectories(Paths.get(storageRoot, "covers"));
            Files.createDirectories(Paths.get(storageRoot, "books"));
            Files.createDirectories(Paths.get(storageRoot, "authors"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directories", e);
        }
    }

    // FIXED: PDF extraction with proper page ordering
    public static List<String> extractTextFromPDF(Path pdfPath) throws IOException {
        List<String> pages = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(pdfPath.toFile());
             PDDocument document = Loader.loadPDF(fis.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            // Process pages in sequential order (1, 2, 3, ...)
            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);
                pages.add(pageText != null ? pageText.trim() : "");

                log.debug("Extracted page {} of {}: {} characters",
                        i, totalPages, pageText != null ? pageText.length() : 0);
            }

            log.info("Extracted {} pages from PDF document in correct order", pages.size());
            return pages;

        } catch (Exception e) {
            log.error("Failed to extract text from PDF document: {}", e.getMessage(), e);
            throw new IOException("Failed to extract text from PDF document", e);
        }
    }

    // FIXED: EPUB extraction following spine order
    public static List<String> extractTextFromEPUB(Path epubPath) throws IOException {
        List<String> pages = new ArrayList<>();

        try (InputStream is = Files.newInputStream(epubPath)) {
            EpubReader epubReader = new EpubReader();
            Book epubBook = epubReader.readEpub(is);

            // FIXED: Use spine references to maintain reading order
            List<SpineReference> spineReferences = epubBook.getSpine().getSpineReferences();

            log.info("Processing EPUB with {} spine references in order", spineReferences.size());

            for (int i = 0; i < spineReferences.size(); i++) {
                SpineReference spineReference = spineReferences.get(i);
                Resource resource = spineReference.getResource();

                try {
                    String mediaType = resource.getMediaType().toString().toLowerCase();

                    if (mediaType.contains("html") || mediaType.contains("xhtml") ||
                            mediaType.startsWith("text/") || mediaType.contains("xml")) {

                        String content = new String(resource.getData(), StandardCharsets.UTF_8);
                        String plainText = Jsoup.parse(content).text();

                        if (!plainText.trim().isEmpty()) {
                            pages.add(plainText.trim());
                            log.debug("Added spine reference {} ({}): {} characters",
                                    i + 1, resource.getHref(), plainText.length());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to process EPUB spine reference {}: {} - Error: {}",
                            i + 1, resource.getHref(), e.getMessage());
                }
            }

            log.info("Extracted {} pages from EPUB document in spine order", pages.size());
            return pages;

        } catch (Exception e) {
            log.error("Failed to extract text from EPUB document: {}", e.getMessage(), e);
            throw new IOException("Failed to extract text from EPUB document", e);
        }
    }

    // FIXED: Word extraction with better paragraph handling
    public static List<String> extractTextFromWord(Path wordPath) throws IOException {
        List<String> pages = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(wordPath.toFile())) {
            XWPFDocument document = new XWPFDocument(fis);

            // FIXED: Extract paragraphs in document order
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            if (paragraphs.isEmpty()) {
                // Fallback to simple text extraction
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                String text = extractor.getText();
                if (text != null && !text.trim().isEmpty()) {
                    pages.add(text.trim());
                }
                extractor.close();
            } else {
                StringBuilder currentPage = new StringBuilder();
                int paragraphCount = 0;
                final int PARAGRAPHS_PER_PAGE = 5; // Adjust based on your needs

                for (XWPFParagraph paragraph : paragraphs) {
                    String paragraphText = paragraph.getText();

                    if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                        if (currentPage.length() > 0) {
                            currentPage.append("\n\n");
                        }
                        currentPage.append(paragraphText.trim());
                        paragraphCount++;

                        // Create new page after certain number of paragraphs
                        if (paragraphCount >= PARAGRAPHS_PER_PAGE) {
                            pages.add(currentPage.toString());
                            currentPage = new StringBuilder();
                            paragraphCount = 0;
                        }
                    }
                }

                // Add remaining content as final page
                if (currentPage.length() > 0) {
                    pages.add(currentPage.toString());
                }
            }

            document.close();

            log.info("Extracted {} pages from Word document in order", pages.size());
            return pages;

        } catch (Exception e) {
            log.error("Failed to extract text from Word document: {}", e.getMessage(), e);
            throw new IOException("Failed to extract text from Word document", e);
        }
    }

    // FIXED: Ensure page creation maintains order
    public static List<ProjectPage> createPDFPagesForOCR(Long projectId, Path filePath, String originalFilename) {
        List<ProjectPage> pages = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             PDDocument document = Loader.loadPDF(fis.readAllBytes())) {

            int totalPages = document.getNumberOfPages();

            // Create pages in sequential order
            for (int i = 0; i < totalPages; i++) {
                ProjectPage page = new ProjectPage();
                page.setProjectId(projectId);
                page.setPageNumber(i + 1); // Page numbers start from 1
                page.setImageUrl(filePath.toString());
                page.setOriginalFilename(originalFilename + " - Page " + (i + 1));
                page.setOcrStatus(OCRStatus.PENDING);
                page.setCreatedAt(LocalDateTime.now());
                page.setUpdatedAt(LocalDateTime.now());

                pages.add(page);

                log.debug("Created OCR page {} for project {}", i + 1, projectId);
            }

            log.info("Created {} OCR pages for PDF in correct order", pages.size());

        } catch (Exception e) {
            log.error("Failed to create PDF pages for OCR: {}", e.getMessage(), e);
        }

        return pages;
    }

    // FIXED: Ensure page creation with text maintains order
    public static List<ProjectPage> createPDFPagesWithText(Long projectId, Path filePath,
                                                           String originalFilename, List<String> extractedContent) {
        List<ProjectPage> pages = new ArrayList<>();

        // Process content in the same order as extraction
        for (int i = 0; i < extractedContent.size(); i++) {
            ProjectPage page = new ProjectPage();
            page.setProjectId(projectId);
            page.setPageNumber(i + 1); // Sequential page numbering
            page.setOriginalFilename(originalFilename + " - Page " + (i + 1));
            page.setTranscribedText(extractedContent.get(i));
            page.setOcrStatus(OCRStatus.COMPLETED);
            page.setOcrConfidence(100.0);
            page.setCreatedAt(LocalDateTime.now());
            page.setUpdatedAt(LocalDateTime.now());

            pages.add(page);

            log.debug("Created PDF text page {} with {} characters",
                    i + 1, extractedContent.get(i).length());
        }

        log.info("Created {} PDF pages with text in correct order", pages.size());
        return pages;
    }

    // FIXED: EPUB page creation in order
    public static List<ProjectPage> createEPUBPages(Long projectId, String originalFilename, List<String> extractedContent) {
        List<ProjectPage> pages = new ArrayList<>();

        // Process content in spine order
        for (int i = 0; i < extractedContent.size(); i++) {
            ProjectPage page = new ProjectPage();
            page.setProjectId(projectId);
            page.setPageNumber(i + 1); // Sequential page numbering
            page.setOriginalFilename(originalFilename + " - Chapter " + (i + 1));
            page.setTranscribedText(extractedContent.get(i));
            page.setOcrStatus(OCRStatus.COMPLETED);
            page.setOcrConfidence(100.0);
            page.setCreatedAt(LocalDateTime.now());
            page.setUpdatedAt(LocalDateTime.now());

            pages.add(page);

            log.debug("Created EPUB page {} with {} characters",
                    i + 1, extractedContent.get(i).length());
        }

        log.info("Created {} EPUB pages in spine order", pages.size());
        return pages;
    }

    // FIXED: Word page creation in order
    public static List<ProjectPage> createWordPages(Long projectId, String originalFilename, List<String> extractedContent) {
        List<ProjectPage> pages = new ArrayList<>();

        // Process content in document order
        for (int i = 0; i < extractedContent.size(); i++) {
            ProjectPage page = new ProjectPage();
            page.setProjectId(projectId);
            page.setPageNumber(i + 1); // Sequential page numbering
            page.setOriginalFilename(originalFilename + " - Page " + (i + 1));
            page.setTranscribedText(extractedContent.get(i));
            page.setOcrStatus(OCRStatus.COMPLETED);
            page.setOcrConfidence(100.0);
            page.setCreatedAt(LocalDateTime.now());
            page.setUpdatedAt(LocalDateTime.now());

            pages.add(page);

            log.debug("Created Word page {} with {} characters",
                    i + 1, extractedContent.get(i).length());
        }

        log.info("Created {} Word pages in document order", pages.size());
        return pages;
    }

    // Rest of the methods remain unchanged...
    public static void validateFile(MultipartFile file, long maxSizeBytes) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not supported: " + extension);
        }

        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("File size exceeds maximum limit");
        }
    }

    public boolean shouldProcessOCR(MultipartFile file) {
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        return OCR_SUPPORTED_EXTENSIONS.contains(extension);
    }

    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public static FileType determineFileType(String extension) {
        extension = extension.toLowerCase();
        switch (extension) {
            case "pdf":
                return FileType.PDF;
            case "doc":
            case "docx":
                return FileType.WORD;
            case "epub":
                return FileType.EPUB;
            case "jpg":
            case "jpeg":
            case "png":
                return FileType.IMAGE;
            default:
                return FileType.OTHER;
        }
    }

    public static long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return 50 * 1024 * 1024; // 50MB default
        }

        String cleanSize = sizeStr.trim().toUpperCase();
        if (cleanSize.endsWith("MB")) {
            return Long.parseLong(cleanSize.replace("MB", "")) * 1024 * 1024;
        } else if (cleanSize.endsWith("KB")) {
            return Long.parseLong(cleanSize.replace("KB", "")) * 1024;
        } else {
            return Long.parseLong(cleanSize); // assume bytes
        }
    }

    public static Path saveFile(MultipartFile file, String uploadDir, Long projectId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;

        Path projectDir = Paths.get(uploadDir, "projects", projectId.toString());
        Files.createDirectories(projectDir);

        Path filePath = projectDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        return filePath;
    }

    public List<ProjectPage> createPDFPages(Long projectId, Path filePath, String originalFilename, List<String> imagePaths) {
        List<ProjectPage> pages = new ArrayList<>();

        for (int i = 0; i < imagePaths.size(); i++) {
            ProjectPage page = new ProjectPage();
            page.setProjectId(projectId);
            page.setPageNumber(i + 1);
            page.setImageUrl(imagePaths.get(i));
            page.setOriginalFilename(originalFilename + " - Page " + (i + 1));
            page.setOcrStatus(OCRStatus.PENDING);
            page.setCreatedAt(LocalDateTime.now());
            page.setUpdatedAt(LocalDateTime.now());

            pages.add(page);
        }

        return pages;
    }

    public static ProjectPage createImagePage(Long projectId, Path filePath, String originalFilename) {
        ProjectPage page = new ProjectPage();
        page.setProjectId(projectId);
        page.setPageNumber(1);
        page.setImageUrl(filePath.toString());
        page.setOriginalFilename(originalFilename);
        page.setOcrStatus(OCRStatus.PENDING);
        page.setCreatedAt(LocalDateTime.now());
        page.setUpdatedAt(LocalDateTime.now());

        return page;
    }

    // Additional utility methods remain the same...
    public static String sanitizeFilename(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    public static String uploadToCloudinary(MultipartFile file, String publicId, String folder, Map<String, Object> transformations) throws IOException {
        Map<String, Object> uploadParams = new HashMap<>();
        uploadParams.put("public_id", publicId);
        uploadParams.put("folder", folder);
        uploadParams.put("use_filename", false);
        uploadParams.put("unique_filename", false);
        uploadParams.put("overwrite", true);

        if (transformations != null) {
            uploadParams.putAll(transformations);
        }

        String resourceType = folder.equals("book_files") ? "raw" : "image";
        uploadParams.put("resource_type", resourceType);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        return (String) uploadResult.get("secure_url");
    }

    // File storage methods...
    public static String uploadBookCover(MultipartFile coverImage, String bookTitle) throws IOException {
        String publicId = sanitizeFilename(bookTitle) + "-cover";

        Map<String, Object> transformations = new HashMap<>();
        transformations.put("transformation", new Transformation()
                .width(800).height(1200)
                .crop("fit")
                .quality("auto:best")
                .fetchFormat("webp"));

        return uploadToCloudinary(coverImage, publicId, "book_covers", transformations);
    }

    public static String uploadBookFile(MultipartFile bookFile, String bookTitle) throws IOException {
        String originalFilename = bookFile.getOriginalFilename();
        String fileExtension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf('.')) : "";

        String publicId = sanitizeFilename(bookTitle) + "-book" + fileExtension;

        return uploadToCloudinary(bookFile, publicId, "book_files", null);
    }

    public static String uploadAuthorPhoto(MultipartFile photo, String authorName) throws IOException {
        String publicId = sanitizeFilename(authorName) + "-author";

        Map<String, Object> transformations = new HashMap<>();
        transformations.put("transformation", new Transformation()
                .width(300).height(300)
                .crop("fill").gravity("face")
                .effect("brightness:20")
                .quality("auto:good")
                .fetchFormat("webp"));

        return uploadToCloudinary(photo, publicId, "author_photos", transformations);
    }

    public static String saveToLocal(MultipartFile file, String directory, String filename) throws IOException {
        Path filePath = Paths.get(storageRoot, directory, filename);
        Files.createDirectories(filePath.getParent());

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.debug("File saved to: {}", filePath);
        return filePath.toString();
    }

    public static String getFilename(MultipartFile file, String baseName, String suffix) {
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        String fileExtension = ".jpg";
        if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else if (contentType != null && contentType.contains("png")) {
            fileExtension = ".png";
        }

        return sanitizeFilename(baseName) + suffix + fileExtension;
    }

    public static String saveBookCover(MultipartFile coverImage, String title) throws IOException {
        String filename = getFilename(coverImage, title, "-cover");
        return saveToLocal(coverImage, "covers", filename);
    }

    public static String saveBookFile(MultipartFile bookFile, String title) throws IOException {
        String originalFilename = bookFile.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.lastIndexOf(".") > 0
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".pdf";

        String filename = sanitizeFilename(title) + "-book" + fileExtension;
        return saveToLocal(bookFile, "books", filename);
    }

    public static String saveAuthorPhoto(MultipartFile authorPhoto, String authorName) throws IOException {
        String filename = getFilename(authorPhoto, authorName, "-author");
        return saveToLocal(authorPhoto, "authors", filename);
    }

    public static FileStorageResult saveAndUploadBookCover(MultipartFile coverImage, String title) throws IOException {
        String localPath = saveBookCover(coverImage, title);
        String cloudUrl = uploadBookCover(coverImage, title);
        return new FileStorageResult(localPath, cloudUrl);
    }

    public static FileStorageResult saveAndUploadBookFile(MultipartFile bookFile, String title) throws IOException {
        String localPath = saveBookFile(bookFile, title);
        String cloudUrl = uploadBookFile(bookFile, title);
        return new FileStorageResult(localPath, cloudUrl);
    }

    public static FileStorageResult saveAndUploadAuthorPhoto(MultipartFile authorPhoto, String authorName) throws IOException {
        String localPath = saveAuthorPhoto(authorPhoto, authorName);
        String cloudUrl = uploadAuthorPhoto(authorPhoto, authorName);
        return new FileStorageResult(localPath, cloudUrl);
    }

    public static int calculateEstimatedReadTime(long totalWord) {
        return (int) Math.max(1, Math.round((double) totalWord / WORDS_PER_MINUTE));
    }

    // BookMetadata extraction and other methods remain unchanged...
    public static BookMetadata extractBookMetadata(MultipartFile bookFile, Integer languageId) throws IOException {
        String originalFilename = bookFile.getOriginalFilename();
        String fileFormat = "";
        long totalWord = 0L;
        int totalPages = 0;
        int difficultyLevel = 1;

        if (originalFilename != null && originalFilename.contains(".")) {
            fileFormat = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        String extractedText = "";

        if ("pdf".equals(fileFormat)) {
            try (InputStream is = bookFile.getInputStream();
                 PDDocument document = Loader.loadPDF(is.readAllBytes())) {

                totalPages = document.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                stripper.setShouldSeparateByBeads(true);
                extractedText = stripper.getText(document);
                totalWord = countWords(extractedText);
                difficultyLevel = calculateDifficultyLevel(extractedText);

                if (languageId != null && languageId != 1) {
                    difficultyLevel = Math.min(5, difficultyLevel + 1);
                }
            }
        } else if ("epub".equals(fileFormat)) {
            try (InputStream is = bookFile.getInputStream()) {
                StringBuilder epubText = new StringBuilder();
                EpubReader epubReader = new EpubReader();
                Book epubBook = epubReader.readEpub(is);

                log.debug("Processing EPUB file: {}", originalFilename);
                log.debug("Total resources found: {}", epubBook.getResources().size());

                int processedResources = 0;
                int totalContentLength = 0;

                for (Resource resource : epubBook.getResources().getAll()) {
                    try {
                        String mediaType = resource.getMediaType().toString().toLowerCase();

                        if (mediaType.contains("html") ||
                                mediaType.contains("xhtml") ||
                                mediaType.startsWith("text/") ||
                                mediaType.contains("xml")) {

                            String content = new String(resource.getData(), StandardCharsets.UTF_8);
                            String plainText = Jsoup.parse(content).text();

                            if (!plainText.trim().isEmpty()) {
                                epubText.append(plainText).append(" ");
                                processedResources++;
                                totalContentLength += plainText.length();

                                log.debug("Processed resource: {} (type: {}) - Content length: {}",
                                        resource.getHref(), mediaType, plainText.length());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to process EPUB resource: {} - Error: {}",
                                resource.getHref(), e.getMessage());

                        try {
                            String fallbackContent = new String(resource.getData(), StandardCharsets.UTF_8);
                            if (fallbackContent.length() > 50) {
                                String plainText = Jsoup.parse(fallbackContent).text();
                                if (!plainText.trim().isEmpty()) {
                                    epubText.append(plainText).append(" ");
                                    processedResources++;
                                    totalContentLength += plainText.length();
                                    log.debug("Fallback success for: {} - Content length: {}",
                                            resource.getHref(), plainText.length());
                                }
                            }
                        } catch (Exception fallbackError) {
                            log.debug("Fallback also failed for: {} - {}",
                                    resource.getHref(), fallbackError.getMessage());
                        }
                    }
                }

                extractedText = epubText.toString();

                log.debug("EPUB Processing Summary:");
                log.debug("- Processed resources: {}/{}", processedResources, epubBook.getResources().size());
                log.debug("- Total extracted text length: {} characters", extractedText.length());
                log.debug("- Average content per resource: {} characters",
                        processedResources > 0 ? totalContentLength / processedResources : 0);

                if (extractedText.length() > 200) {
                    log.debug("Sample text (first 200 chars): {}", extractedText.substring(0, 200));
                }

                totalWord = countWords(extractedText);
                difficultyLevel = calculateDifficultyLevel(extractedText);

                if (languageId != null && languageId != 1) {
                    difficultyLevel = Math.min(5, difficultyLevel + 1);
                }

                log.debug("Final word count: {}", totalWord);
            }
        }

        log.debug("Extracted metadata - Words: {}, Pages: {}, Difficulty: {}", totalWord, totalPages, difficultyLevel);

        return new BookMetadata(fileFormat, bookFile.getSize(), totalPages, totalWord, difficultyLevel);
    }

    public static void deleteFile(String filePathOrUrl) {
        if (filePathOrUrl == null || filePathOrUrl.trim().isEmpty()) {
            return;
        }

        try {
            // Handle Cloudinary URLs
            if (filePathOrUrl.contains("cloudinary.com") || filePathOrUrl.contains("res.cloudinary.com")) {
                // Extract public ID from Cloudinary URL
                String publicId = null;
                String pattern = "cloudinary.com/[^/]+/(?:image|raw)/upload/(?:v\\d+/)?(.*?)(?:\\.[^.]+)?$";
                java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = r.matcher(filePathOrUrl);

                if (m.find()) {
                    publicId = m.group(1);
                    if (publicId.contains(".")) {
                        publicId = publicId.substring(0, publicId.lastIndexOf('.'));
                    }
                }

                if (publicId != null) {
                    String resourceType = filePathOrUrl.contains("/book_files/") ? "raw" : "image";
                    Map<String, Object> options = new HashMap<>();
                    options.put("resource_type", resourceType);
                    cloudinary.uploader().destroy(publicId, options);
                    log.debug("Deleted Cloudinary file: {}", filePathOrUrl);
                }
            } else {
                // Handle local files
                Path path = Paths.get(filePathOrUrl);
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.debug("Deleted local file: {}", filePathOrUrl);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to delete file: {} - Error: {}", filePathOrUrl, e.getMessage());
        }
    }

    private static long countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0L;
        }

        String cleanText = text.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\p{Punct}&&[^'-]]", " ")
                .replaceAll("\\s+", " ");

        if (cleanText.isEmpty()) {
            return 0L;
        }

        String[] words = cleanText.split("\\s+");

        long wordCount = Arrays.stream(words)
                .filter(word -> word.length() > 0 && word.matches(".*[\\p{L}\\p{N}].*"))
                .count();

        log.debug("Word counting: {} raw words -> {} valid words", words.length, wordCount);
        return wordCount;
    }

    private static int calculateDifficultyLevel(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 1;
        }

        String cleanText = text.replaceAll("[0-9]", "1")
                .replaceAll("(?i)\\b([a-z]{1,2}|[0-9]+)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();

        String[] sentences = cleanText.split("[.!?]\\s+");
        String[] words = cleanText.split("\\s+");

        if (sentences.length == 0 || words.length < 10) {
            return 1;
        }

        int syllableCount = countSyllables(cleanText);
        int complexWords = countComplexWords(words);

        double gunningFog = calculateGunningFog(sentences, words, complexWords);
        double fleschKincaid = calculateFleschKincaid(sentences, words, syllableCount);
        double powerSumnerKearl = calculatePowerSumnerKearl(sentences, words, syllableCount);

        double averageLevel = (gunningFog + fleschKincaid + powerSumnerKearl) / 3;

        log.debug("Difficulty calculation - Gunning Fog: {}, Flesch-Kincaid: {}, Power Sumner Kearl: {}, Average: {}",
                gunningFog, fleschKincaid, powerSumnerKearl, averageLevel);

        return convertToDifficultyScale(averageLevel);
    }

    private static int countSyllables(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        return Arrays.stream(text.toLowerCase().split("\\s+"))
                .filter(word -> !word.trim().isEmpty())
                .mapToInt(FileUtil::countSyllablesInWord)  // Changed to static reference
                .sum();
    }

    private static int countSyllablesInWord(String word) {
        word = word.replaceAll("[^a-z]", "").toLowerCase();

        if (word.length() == 0) return 0;
        if (word.length() <= 3) return 1;

        word = word.replaceAll("e$", "");

        String[] vowelGroups = word.split("[^aeiouy]+");
        int syllables = 0;

        for (String group : vowelGroups) {
            if (!group.isEmpty()) {
                syllables++;
            }
        }

        return Math.max(1, syllables);
    }

    private static int countComplexWords(String[] words) {
        return (int) Arrays.stream(words)
                .filter(word -> countSyllablesInWord(word) >= 3)
                .count();
    }

    private static double calculateGunningFog(String[] sentences, String[] words, int complexWords) {
        double avgSentenceLength = (double) words.length / sentences.length;
        double percentComplex = (double) complexWords / words.length * 100;

        double result = (avgSentenceLength + percentComplex) * 0.4;

        log.debug("Gunning Fog calculation - Avg sentence length: {}, Complex words %: {}, Result: {}",
                avgSentenceLength, percentComplex, result);

        return result;
    }

    private static double calculateFleschKincaid(String[] sentences, String[] words, int syllableCount) {
        double avgSentenceLength = (double) words.length / sentences.length;
        double avgSyllablesPerWord = (double) syllableCount / words.length;

        double x = avgSentenceLength * 1.015;
        double y = avgSyllablesPerWord * 84.6;
        double readingEase = 206.835 - (x + y);

        double gradeLevel;
        if (readingEase >= 90) {
            gradeLevel = 4.5;
        } else if (readingEase >= 80) {
            gradeLevel = 5.5;
        } else if (readingEase >= 70) {
            gradeLevel = 7.0;
        } else if (readingEase >= 60) {
            gradeLevel = 8.5;
        } else if (readingEase >= 50) {
            gradeLevel = 12.0;
        } else if (readingEase >= 30) {
            gradeLevel = 16.0;
        } else {
            gradeLevel = 18.0;
        }

        log.debug("Flesch-Kincaid calculation - Reading Ease: {}, Grade Level: {}", readingEase, gradeLevel);

        return gradeLevel;
    }

    private static double calculatePowerSumnerKearl(String[] sentences, String[] words, int syllableCount) {
        double x = (double) words.length / sentences.length;
        double y = syllableCount;

        double result = (x * 0.0778) + (y * 0.0455) - 2.2029;

        log.debug("Power Sumner Kearl calculation - x: {}, y: {}, Result: {}", x, y, result);

        return result;
    }

    private static int convertToDifficultyScale(double gradeLevel) {
        if (gradeLevel < 2) return 1;
        if (gradeLevel < 3) return 2;
        if (gradeLevel < 4) return 3;
        if (gradeLevel < 5) return 4;
        if (gradeLevel < 6) return 5;
        if (gradeLevel < 8) return 6;
        if (gradeLevel < 10) return 7;
        if (gradeLevel < 12) return 8;
        if (gradeLevel < 16) return 9;
        return 10;
    }
}