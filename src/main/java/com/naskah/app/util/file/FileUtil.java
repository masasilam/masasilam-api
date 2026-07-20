package com.naskah.app.util.file;

import com.naskah.app.model.dto.BookMetadata;
import com.naskah.app.model.dto.FileStorageResult;
import com.naskah.app.model.entity.ProjectPage;
import com.naskah.app.model.enums.FileType;
import com.naskah.app.model.enums.OCRStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUtil {
    private final VpsFileStorage vpsStorage;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "epub", "pdf", "doc", "docx");
    private static final int WORDS_PER_MINUTE = 200;
    private static final String PAGE = " - Page ";
    private static final String FOLDER_BOOKS = "books";
    private static final String FOLDER_COVERS = "covers";
    private static final String FOLDER_AUTHORS = "authors";
    private static final String FOLDER_CHAPTERS = "chapters";
    private static final String FOLDER_BLOG = "blog-images";
    private static final String FOLDER_PRODUCTS = "product-images";
    private static final String WEBP = ".webp";
    private static final String DELIMETER = "/";


    public FileStorageResult saveAndUploadBookFile(MultipartFile bookFile, String title) throws IOException {
        String ext = getFileExtension(bookFile.getOriginalFilename());
        String filename = sanitizeFilename(title) + "." + ext;
        String remotePath = FOLDER_BOOKS + DELIMETER + filename;

        String url = vpsStorage.upload(bookFile.getBytes(), remotePath);
        return new FileStorageResult(url);
    }

    public String uploadBookCoverFromBytes(byte[] imageData, String bookTitle, Long bookId) throws IOException {
        String baseName = "cover-" + sanitizeFilename(bookTitle);
        String filename = baseName + WEBP;
        String remotePath = FOLDER_COVERS + DELIMETER + filename;
        if (vpsStorage.exists(remotePath)) {
            filename = baseName + "-" + bookId + WEBP;
            remotePath = FOLDER_COVERS + DELIMETER + filename;
            log.info("Cover name conflict, using: {}", filename);
        }

        return vpsStorage.upload(imageData, remotePath);
    }

    public String uploadChapterImageFromBytes(byte[] imageData, Long bookId, String fileName) {
        String nameNoExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String filename = bookId + "-" + sanitizeFilename(nameNoExt) + WEBP;
        String remotePath = FOLDER_CHAPTERS + DELIMETER + filename;
        return vpsStorage.upload(imageData, remotePath);
    }

    public String uploadAuthorPhoto(MultipartFile photo, String authorName) throws IOException {
        String ext = getFileExtension(photo.getOriginalFilename());
        String filename = sanitizeFilename(authorName) + "-author." + ext;
        String remotePath = FOLDER_AUTHORS + DELIMETER + filename;
        return vpsStorage.upload(photo.getBytes(), remotePath);
    }

    public String uploadBlogImage(MultipartFile image, Long postId) throws IOException {
        String ext = getFileExtension(image.getOriginalFilename());
        String filename = (postId != null ? postId : "draft") + "-" + System.currentTimeMillis() + "." + ext;
        String remotePath = FOLDER_BLOG + DELIMETER + filename;
        return vpsStorage.upload(image.getBytes(), remotePath);
    }

    public String uploadProductImage(MultipartFile image, String productName) throws IOException {
        String ext = getFileExtension(image.getOriginalFilename());
        String filename = sanitizeFilename(productName) + "-" + System.currentTimeMillis() + "." + ext;
        String remotePath = FOLDER_PRODUCTS + DELIMETER + filename;
        return vpsStorage.upload(image.getBytes(), remotePath);
    }

    public FileStorageResult saveAndUploadProductImage(MultipartFile image, String productName) throws IOException {
        return new FileStorageResult(uploadProductImage(image, productName));
    }

    public String uploadEpubOverwrite(byte[] epubBytes, String bookSlug) {
        String remotePath = FOLDER_BOOKS + DELIMETER + sanitizeFilename(bookSlug) + ".epub";
        return vpsStorage.upload(epubBytes, remotePath);
    }

    public void deleteFile(String filePathOrUrl) {
        if (filePathOrUrl == null || filePathOrUrl.isBlank()) return;

        if (filePathOrUrl.startsWith("http")) {
            vpsStorage.delete(filePathOrUrl);
        } else {
            try {
                Path path = Paths.get(filePathOrUrl);
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.debug("Deleted local file: {}", filePathOrUrl);
                }
            } catch (IOException e) {
                log.warn("Failed to delete local file '{}': {}", filePathOrUrl, e.getMessage());
            }
        }
    }

    public byte[] downloadFromUrl(String urlString) throws IOException {
        if (urlString == null || urlString.isBlank()) throw new IllegalArgumentException("URL cannot be blank");

        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(urlString);
            URL url = uri.toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(30_000);
            connection.setRequestProperty("User-Agent", "MasasilamApp/1.0");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + connection.getResponseCode() + " from " + urlString);
            }

            try (InputStream is = connection.getInputStream()) {
                return is.readAllBytes();
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public Path saveFile(MultipartFile file, String uploadDir, Long projectId) throws IOException {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String uniqueFilename = UUID.randomUUID() + "." + fileExtension;
        Path projectDir = Paths.get(uploadDir, "projects", projectId.toString());
        Files.createDirectories(projectDir);
        Path filePath = projectDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);
        return filePath;
    }

    public List<String> extractTextFromPDF(Path pdfPath) throws IOException {
        List<String> pages = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(pdfPath.toFile());
             PDDocument document = Loader.loadPDF(fis.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            int total = document.getNumberOfPages();
            for (int i = 1; i <= total; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document);
                pages.add(text != null ? text.trim() : "");
            }
        }
        return pages;
    }

    public List<String> extractTextFromWord(Path wordPath) throws IOException {
        List<String> pages = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(wordPath.toFile())) {
            XWPFDocument document = new XWPFDocument(fis);
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            if (paragraphs.isEmpty()) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                String text = extractor.getText();
                if (text != null && !text.trim().isEmpty()) pages.add(text.trim());
                extractor.close();
            } else {
                StringBuilder currentPage = new StringBuilder();
                int count = 0;
                for (XWPFParagraph p : paragraphs) {
                    String t = p.getText();
                    if (t != null && !t.trim().isEmpty()) {
                        if (!currentPage.isEmpty()) currentPage.append("\n\n");
                        currentPage.append(t.trim());
                        if (++count >= 5) {
                            pages.add(currentPage.toString());
                            currentPage = new StringBuilder();
                            count = 0;
                        }
                    }
                }
                if (!currentPage.isEmpty()) pages.add(currentPage.toString());
            }
            document.close();
        }
        return pages;
    }

    public int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return (int) Arrays.stream(text.trim().replaceAll("\\s+", " ").split("\\s+"))
                .filter(w -> !w.isEmpty() && w.matches(".*[\\p{L}\\p{N}].*"))
                .count();
    }

    public String generatePreviewText(String content, int maxLength) {
        if (content == null || content.isEmpty()) return "";
        int len = Math.min(maxLength, content.length());
        String prev = content.substring(0, len);
        int lastDot = prev.lastIndexOf('.');
        return (lastDot > maxLength / 2 ? prev.substring(0, lastDot + 1) : prev + "...").trim();
    }

    public int calculateEstimatedReadTime(long totalWord) {
        return (int) Math.max(1, Math.round((double) totalWord / WORDS_PER_MINUTE));
    }

    public void validateFile(MultipartFile file, long maxSizeBytes) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty())
            throw new IllegalArgumentException("Filename cannot be empty");
        if (!ALLOWED_EXTENSIONS.contains(getFileExtension(filename).toLowerCase()))
            throw new IllegalArgumentException("File type not supported");
        if (file.getSize() > maxSizeBytes) throw new IllegalArgumentException("File size exceeds maximum limit");
    }

    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public FileType determineFileType(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf" -> FileType.PDF;
            case "doc", "docx" -> FileType.WORD;
            case "epub" -> FileType.EPUB;
            case "jpg", "jpeg", "png" -> FileType.IMAGE;
            default -> FileType.OTHER;
        };
    }

    public long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) return 50L * 1024L * 1024L;
        String clean = sizeStr.trim().toUpperCase();
        if (clean.endsWith("MB")) return Long.parseLong(clean.replace("MB", "")) * 1024 * 1024;
        if (clean.endsWith("KB")) return Long.parseLong(clean.replace("KB", "")) * 1024;
        return Long.parseLong(clean);
    }

    public String sanitizeFilename(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-)|(-$)", "");
    }

    public BookMetadata extractBookMetadata(MultipartFile bookFile) throws IOException {
        String originalFilename = bookFile.getOriginalFilename();
        String fileFormat = "";
        long totalWord = 0L;
        int totalPages = 0;

        if (originalFilename != null && originalFilename.contains("."))
            fileFormat = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        if ("pdf".equals(fileFormat)) {
            try (InputStream is = bookFile.getInputStream();
                 PDDocument document = Loader.loadPDF(is.readAllBytes())) {
                totalPages = document.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                totalWord = countWords(stripper.getText(document));
            }
        }

        return new BookMetadata(fileFormat, bookFile.getSize(), totalPages, totalWord);
    }

    public List<ProjectPage> createPDFPagesForOCR(Long projectId, Path filePath, String originalFilename) {
        List<ProjectPage> pages = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             PDDocument document = Loader.loadPDF(fis.readAllBytes())) {
            int total = document.getNumberOfPages();
            for (int i = 0; i < total; i++) {
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
        } catch (Exception e) {
            log.error("Failed to create PDF pages for OCR: {}", e.getMessage());
        }
        return pages;
    }

    public List<ProjectPage> createPDFPagesWithText(Long projectId, String originalFilename, List<String> content) {
        return buildPages(projectId, originalFilename, content);
    }

    public List<ProjectPage> createWordPages(Long projectId, String originalFilename, List<String> content) {
        return buildPages(projectId, originalFilename, content);
    }

    private List<ProjectPage> buildPages(Long projectId, String filename, List<String> content) {
        List<ProjectPage> pages = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            ProjectPage page = new ProjectPage();
            page.setProjectId(projectId);
            page.setPageNumber(i + 1);
            page.setOriginalFilename(filename + PAGE + (i + 1));
            page.setTranscribedText(content.get(i));
            page.setOcrStatus(OCRStatus.COMPLETED);
            page.setOcrConfidence(100.0);
            page.setCreatedAt(LocalDateTime.now());
            page.setUpdatedAt(LocalDateTime.now());
            pages.add(page);
        }
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

    public String toTitleCase(String text) {
        if (text == null || text.isEmpty()) return text;
        List<String> lowercase = Arrays.asList("di", "ke", "dari", "tentang", "dan", "atau", "karena", "yang", "oh", "dong", "kok", "sih", "si", "sang", "pun", "per");
        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            String word = words[i];
            if (word.matches("^[ivxlcdm]+\\.?$")) result.append(word.toUpperCase());
            else if (i == 0) result.append(capitalizeFirstLetter(word));
            else if (lowercase.contains(word)) result.append(word);
            else result.append(capitalizeFirstLetter(word));
        }
        return result.toString();
    }

    private String capitalizeFirstLetter(String word) {
        if (word == null || word.isEmpty()) return word;
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }
}