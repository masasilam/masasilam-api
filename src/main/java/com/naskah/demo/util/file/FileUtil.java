package com.naskah.demo.util.file;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.naskah.demo.model.dto.BookMetadata;
import com.naskah.demo.model.dto.FileStorageResult;
import com.naskah.demo.model.entity.ProjectPage;
import com.naskah.demo.model.enums.FileType;
import com.naskah.demo.model.enums.OCRStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class FileUtil {
    private final Cloudinary cloudinary;
    private static final String STORAGE_ROOT = Paths.get(System.getProperty("user.dir"), "storage").toString();
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "epub", "pdf", "doc", "docx");
    private static final int WORDS_PER_MINUTE = 200;
    private static final String TRANSFORMATION = "transformation";
    private static final String AUTO_GOOD = "auto:good";
    private static final String PAGE = " - Page ";
    private static final String RESOURCE_TYPE = "resource_type";
    private static final String BOOK_FILES = "book_files";
    private static final String IMAGE = "image";

    public FileUtil(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
        try {
            Files.createDirectories(Paths.get(STORAGE_ROOT, "covers"));
            Files.createDirectories(Paths.get(STORAGE_ROOT, "books"));
            Files.createDirectories(Paths.get(STORAGE_ROOT, "authors"));
        } catch (IOException e) {
            log.error("Failed to create storage directories", e);
        }
    }

    // ==================== TEXT EXTRACTION ====================

    public List<String> extractTextFromPDF(Path pdfPath) throws IOException {
        List<String> pages = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(pdfPath.toFile());
             PDDocument document = Loader.loadPDF(fis.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);
                pages.add(pageText != null ? pageText.trim() : "");
            }

            log.info("Extracted {} pages from PDF", pages.size());
            return pages;
        } catch (Exception e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage(), e);
            throw new IOException("Failed to extract text from PDF", e);
        }
    }

    public List<String> extractTextFromWord(Path wordPath) throws IOException {
        List<String> pages = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(wordPath.toFile())) {
            XWPFDocument document = new XWPFDocument(fis);
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            if (paragraphs.isEmpty()) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                String text = extractor.getText();
                if (text != null && !text.trim().isEmpty()) {
                    pages.add(text.trim());
                }
                extractor.close();
            } else {
                StringBuilder currentPage = new StringBuilder();
                int paragraphCount = 0;
                final int PARAGRAPHS_PER_PAGE = 5;

                for (XWPFParagraph paragraph : paragraphs) {
                    String paragraphText = paragraph.getText();
                    if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                        if (!currentPage.isEmpty()) {
                            currentPage.append("\n\n");
                        }
                        currentPage.append(paragraphText.trim());
                        paragraphCount++;

                        if (paragraphCount >= PARAGRAPHS_PER_PAGE) {
                            pages.add(currentPage.toString());
                            currentPage = new StringBuilder();
                            paragraphCount = 0;
                        }
                    }
                }

                if (!currentPage.isEmpty()) {
                    pages.add(currentPage.toString());
                }
            }

            document.close();
            log.info("Extracted {} pages from Word", pages.size());
            return pages;
        } catch (Exception e) {
            log.error("Failed to extract text from Word: {}", e.getMessage(), e);
            throw new IOException("Failed to extract text from Word", e);
        }
    }

    // ==================== TEXT UTILITIES ====================

    public int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        String cleanText = text.trim().replaceAll("\\s+", " ");
        String[] words = cleanText.split("\\s+");

        return (int) Arrays.stream(words).filter(word -> !word.isEmpty() && word.matches(".*[\\p{L}\\p{N}].*")).count();
    }

    public String generatePreviewText(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        int previewLength = Math.min(maxLength, content.length());
        String preview = content.substring(0, previewLength);

        int lastPeriod = preview.lastIndexOf('.');
        if (lastPeriod > maxLength / 2) {
            preview = preview.substring(0, lastPeriod + 1);
        } else {
            preview = preview + "...";
        }

        return preview.trim();
    }

    public String uploadChapterImageFromBytes(byte[] imageData, Long bookId, String fileName) throws IOException {
        // Remove extension untuk public_id
        String nameWithoutExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

        String sanitizedName = sanitizeFilename(nameWithoutExt);
        String publicId = String.format("books/%d/chapters/%s", bookId, sanitizedName);

        Map<String, Object> transformations = new HashMap<>();
        transformations.put(TRANSFORMATION, new Transformation<>()
                .width(1000)              // Max width 1000px
                .crop("limit")            // Don't crop, just resize if needed
                .quality(AUTO_GOOD)     // Auto quality optimization
                .fetchFormat("webp"));    // Convert to WebP for better compression

        return uploadBytesToCloudinary(imageData, publicId, "book_chapters", transformations);
    }

    // ==================== PROJECT PAGE CREATION ====================

    public List<ProjectPage> createPDFPagesForOCR(Long projectId, Path filePath, String originalFilename) {
        List<ProjectPage> pages = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             PDDocument document = Loader.loadPDF(fis.readAllBytes())) {

            int totalPages = document.getNumberOfPages();
            for (int i = 0; i < totalPages; i++) {
                ProjectPage page = new ProjectPage();
                page.setProjectId(projectId);
                page.setPageNumber(i + 1);
                page.setImageUrl(filePath.toString());
                page.setOriginalFilename(originalFilename + PAGE + (i + 1));
                page.setOcrStatus(OCRStatus.PENDING);
                page.setCreatedAt(LocalDateTime.now());
                page.setUpdatedAt(LocalDateTime.now());
                pages.add(page);
            }

            log.info("Created {} OCR pages for PDF", pages.size());
        } catch (Exception e) {
            log.error("Failed to create PDF pages for OCR: {}", e.getMessage(), e);
        }
        return pages;
    }

    public List<ProjectPage> createPDFPagesWithText(Long projectId, String originalFilename, List<String> extractedContent) {
        List<ProjectPage> pages = new ArrayList<>();
        for (int i = 0; i < extractedContent.size(); i++) {
            ProjectPage page = new ProjectPage();
            page.setProjectId(projectId);
            page.setPageNumber(i + 1);
            page.setOriginalFilename(originalFilename + PAGE + (i + 1));
            page.setTranscribedText(extractedContent.get(i));
            page.setOcrStatus(OCRStatus.COMPLETED);
            page.setOcrConfidence(100.0);
            page.setCreatedAt(LocalDateTime.now());
            page.setUpdatedAt(LocalDateTime.now());
            pages.add(page);
        }

        log.info("Created {} PDF pages with text", pages.size());
        return pages;
    }

    public List<ProjectPage> createWordPages(Long projectId, String originalFilename, List<String> extractedContent) {
        List<ProjectPage> pages = new ArrayList<>();
        for (int i = 0; i < extractedContent.size(); i++) {
            ProjectPage page = new ProjectPage();
            page.setProjectId(projectId);
            page.setPageNumber(i + 1);
            page.setOriginalFilename(originalFilename + PAGE + (i + 1));
            page.setTranscribedText(extractedContent.get(i));
            page.setOcrStatus(OCRStatus.COMPLETED);
            page.setOcrConfidence(100.0);
            page.setCreatedAt(LocalDateTime.now());
            page.setUpdatedAt(LocalDateTime.now());
            pages.add(page);
        }

        log.info("Created {} Word pages", pages.size());
        return pages;
    }

    public ProjectPage createImagePage(Long projectId, Path filePath, String originalFilename) {
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

    // ==================== FILE VALIDATION ====================

    public void validateFile(MultipartFile file, long maxSizeBytes) {
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

    // ==================== FILE UTILITIES ====================

    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public FileType determineFileType(String extension) {
        extension = extension.toLowerCase();
        return switch (extension) {
            case "pdf" -> FileType.PDF;
            case "doc", "docx" -> FileType.WORD;
            case "epub" -> FileType.EPUB;
            case "jpg", "jpeg", "png" -> FileType.IMAGE;
            default -> FileType.OTHER;
        };
    }

    public long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return 50L * 1024L * 1024L;
        }

        String cleanSize = sizeStr.trim().toUpperCase();
        if (cleanSize.endsWith("MB")) {
            return Long.parseLong(cleanSize.replace("MB", "")) * 1024 * 1024;
        } else if (cleanSize.endsWith("KB")) {
            return Long.parseLong(cleanSize.replace("KB", "")) * 1024;
        } else {
            return Long.parseLong(cleanSize);
        }
    }

    public String sanitizeFilename(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-)|(-$)", "");
    }

    public int calculateEstimatedReadTime(long totalWord) {
        return (int) Math.max(1, Math.round((double) totalWord / WORDS_PER_MINUTE));
    }

    // ==================== CLOUDINARY UPLOAD ====================

    private String uploadToCloudinary(MultipartFile file, String publicId, String folder, Map<String, Object> transformations) throws IOException {
        Map<String, Object> uploadParams = new HashMap<>();
        uploadParams.put("public_id", publicId);
        uploadParams.put("folder", folder);
        uploadParams.put("use_filename", false);
        uploadParams.put("unique_filename", false);
        uploadParams.put("overwrite", true);

        if (transformations != null) {
            uploadParams.putAll(transformations);
        }

        String resourceType = folder.equals(BOOK_FILES) ? "raw" : IMAGE;
        uploadParams.put(RESOURCE_TYPE, resourceType);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        return (String) uploadResult.get("secure_url");
    }

    private String uploadBytesToCloudinary(byte[] bytes, String publicId, String folder, Map<String, Object> transformations) throws IOException {
        Map<String, Object> uploadParams = new HashMap<>();
        uploadParams.put("public_id", publicId);
        uploadParams.put("folder", folder);
        uploadParams.put("use_filename", false);
        uploadParams.put("unique_filename", false);
        uploadParams.put("overwrite", true);

        if (transformations != null) {
            uploadParams.putAll(transformations);
        }

        String resourceType = folder.equals(BOOK_FILES) ? "raw" : IMAGE;
        uploadParams.put(RESOURCE_TYPE, resourceType);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(bytes, uploadParams);
        return (String) uploadResult.get("secure_url");
    }

    // ==================== BOOK FILE UPLOADS ====================

    public String uploadBookCover(MultipartFile coverImage, String bookTitle) throws IOException {
        String publicId = sanitizeFilename(bookTitle) + "-cover";
        Map<String, Object> transformations = new HashMap<>();
        transformations.put(TRANSFORMATION, new Transformation<>()
                .width(800).height(1200)
                .crop("fit")
                .quality("auto:best")
                .fetchFormat("webp"));

        return uploadToCloudinary(coverImage, publicId, "book_covers", transformations);
    }

    public String uploadBookCoverFromBytes(byte[] imageData, String bookTitle, Long bookId) throws IOException {
        String publicId = String.format("books/%d/cover-%s", bookId, sanitizeFilename(bookTitle));

        Map<String, Object> transformations = new HashMap<>();
        transformations.put(TRANSFORMATION, new Transformation<>()
                .width(800).height(1200)
                .crop("fit")
                .quality(AUTO_GOOD)
                .fetchFormat("webp"));

        return uploadBytesToCloudinary(imageData, publicId, "book_covers", transformations);
    }

    public String uploadBookFile(MultipartFile bookFile, String bookTitle) throws IOException {
        String originalFilename = bookFile.getOriginalFilename();
        String fileExtension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
        String publicId = sanitizeFilename(bookTitle) + "-book" + fileExtension;

        return uploadToCloudinary(bookFile, publicId, BOOK_FILES, null);
    }

    public String uploadAuthorPhoto(MultipartFile photo, String authorName) throws IOException {
        String publicId = sanitizeFilename(authorName) + "-author";
        Map<String, Object> transformations = new HashMap<>();
        transformations.put(TRANSFORMATION, new Transformation<>()
                .width(300).height(300)
                .crop("fill").gravity("face")
                .effect("brightness:20")
                .quality(AUTO_GOOD)
                .fetchFormat("webp"));

        return uploadToCloudinary(photo, publicId, "author_photos", transformations);
    }

    // ==================== LOCAL STORAGE ====================

    public Path saveFile(MultipartFile file, String uploadDir, Long projectId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + "." + fileExtension;

        Path projectDir = Paths.get(uploadDir, "projects", projectId.toString());
        Files.createDirectories(projectDir);

        Path filePath = projectDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        return filePath;
    }

    // ==================== FILE STORAGE WRAPPERS ====================

    public FileStorageResult saveAndUploadBookFile(MultipartFile bookFile, String title) throws IOException {
        String cloudUrl = uploadBookFile(bookFile, title);
        return new FileStorageResult(cloudUrl);
    }

    public FileStorageResult saveAndUploadAuthorPhoto(MultipartFile authorPhoto, String authorName) throws IOException {
        String cloudUrl = uploadAuthorPhoto(authorPhoto, authorName);
        return new FileStorageResult(cloudUrl);
    }

    // ==================== PRODUCT IMAGE METHODS ====================

    public String uploadProductImage(MultipartFile image, String productName) throws IOException {
        String publicId = sanitizeFilename(productName) + "-product-" + System.currentTimeMillis();
        Map<String, Object> transformations = new HashMap<>();
        transformations.put(TRANSFORMATION, new Transformation<>()
                .width(1000).height(1000)
                .crop("limit")
                .quality(AUTO_GOOD)
                .fetchFormat("webp")
                .effect("sharpen:100"));

        return uploadToCloudinary(image, publicId, "product_images", transformations);
    }

    public FileStorageResult saveAndUploadProductImage(MultipartFile image, String productName) throws IOException {
        String cloudUrl = uploadProductImage(image, productName);
        return new FileStorageResult(cloudUrl);
    }

    // ==================== FILE DELETION ====================

    public void deleteFile(String filePathOrUrl) {
        if (filePathOrUrl == null || filePathOrUrl.trim().isEmpty()) {
            return;
        }

        try {
            if (filePathOrUrl.contains("cloudinary.com") || filePathOrUrl.contains("res.cloudinary.com")) {
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
                    String resourceType = filePathOrUrl.contains("/book_files/") ? "raw" : IMAGE;
                    Map<String, Object> options = new HashMap<>();
                    options.put(RESOURCE_TYPE, resourceType);
                    cloudinary.uploader().destroy(publicId, options);
                    log.debug("Deleted Cloudinary file: {}", filePathOrUrl);
                }
            } else {
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

    // ==================== BOOK METADATA EXTRACTION ====================

    public BookMetadata extractBookMetadata(MultipartFile bookFile) throws IOException {
        String originalFilename = bookFile.getOriginalFilename();
        String fileFormat = "";
        long totalWord = 0L;
        int totalPages = 0;

        if (originalFilename != null && originalFilename.contains(".")) {
            fileFormat = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        String extractedText;

        if ("pdf".equals(fileFormat)) {
            try (InputStream is = bookFile.getInputStream();
                 PDDocument document = Loader.loadPDF(is.readAllBytes())) {

                totalPages = document.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                stripper.setShouldSeparateByBeads(true);
                extractedText = stripper.getText(document);
                totalWord = countWords(extractedText);
            }
        }

        log.debug("Extracted metadata - Words: {}, Pages: {}", totalWord, totalPages);
        return new BookMetadata(fileFormat, bookFile.getSize(), totalPages, totalWord);
    }
}