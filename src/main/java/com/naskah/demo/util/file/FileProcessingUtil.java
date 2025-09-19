package com.naskah.demo.util.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class FileProcessingUtil {

    private static final String UPLOAD_DIR = "uploads/";
    private static final String TTS_DIR = "tts/";
    private static final String VOICE_NOTES_DIR = "voice-notes/";
    private static final String EXPORTS_DIR = "exports/";

    /**
     * Save TTS audio file
     */
    public String saveTTSFile(String fileName, byte[] audioData) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR + TTS_DIR);
        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, audioData);

        return "/api/files/tts/" + fileName;
    }

    /**
     * Save voice note file
     */
    public String saveVoiceNoteFile(MultipartFile audioFile, String fileName) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR + VOICE_NOTES_DIR);
        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(audioFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/api/files/voice-notes/" + fileName;
    }

    /**
     * Save export file
     */
    public String saveExportFile(String fileName, String content, String format) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR + EXPORTS_DIR);
        Files.createDirectories(uploadPath);

        String timestampedFileName = addTimestampToFileName(fileName);
        Path filePath = uploadPath.resolve(timestampedFileName);
        Files.write(filePath, content.getBytes());

        return "/api/files/exports/" + timestampedFileName;
    }

    /**
     * Generate quote image (simplified implementation)
     */
    public String generateQuoteImage(String text, String template, String backgroundColor, String textColor) {
        try {
            // In real implementation, use image generation library like BufferedImage
            // For now, return a placeholder URL
            String fileName = "quote_" + System.currentTimeMillis() + ".png";

            // Save placeholder image data
            byte[] placeholderImage = generatePlaceholderImage(text);
            Path uploadPath = Paths.get(UPLOAD_DIR + "quotes/");
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, placeholderImage);

            return "/api/files/quotes/" + fileName;

        } catch (IOException e) {
            log.error("Error generating quote image", e);
            return "/api/files/quotes/default-quote.png";
        }
    }

    /**
     * Read book content from file
     */
    public String readBookContent(String fileUrl) throws IOException {
        // Implementation depends on book format (EPUB, PDF, TXT)
        // This is a simplified version
        try {
            if (fileUrl.startsWith("http")) {
                // Handle remote files
                return "Remote book content...";
            } else {
                // Handle local files
                Path filePath = Paths.get(fileUrl);
                if (Files.exists(filePath)) {
                    return Files.readString(filePath);
                }
            }
        } catch (Exception e) {
            log.error("Error reading book content from: {}", fileUrl, e);
        }

        return "Book content could not be loaded.";
    }

    /**
     * Get specific page content
     */
    public String getPageContent(String fileUrl, int page) {
        try {
            String fullContent = readBookContent(fileUrl);
            // Simple page splitting - in real implementation, use proper book parsing
            String[] pages = fullContent.split("\\n\\n"); // Split by paragraphs

            if (page > 0 && page <= pages.length) {
                return pages[page - 1];
            }

            return "Page " + page + " not found.";

        } catch (Exception e) {
            log.error("Error getting page content", e);
            return "Page content could not be loaded.";
        }
    }

    /**
     * Get content for a range of pages
     */
    public String getPageRangeContent(String fileUrl, Integer startPage, Integer endPage) {
        try {
            String fullContent = readBookContent(fileUrl);
            String[] pages = fullContent.split("\\n\\n");

            int start = (startPage != null && startPage > 0) ? startPage - 1 : 0;
            int end = (endPage != null && endPage <= pages.length) ? endPage : pages.length;

            StringBuilder rangeContent = new StringBuilder();
            for (int i = start; i < end; i++) {
                rangeContent.append(pages[i]).append("\n\n");
            }

            return rangeContent.toString();

        } catch (Exception e) {
            log.error("Error getting page range content", e);
            return "Content could not be loaded.";
        }
    }

    // Helper methods

    private String addTimestampToFileName(String fileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex) + "_" + timestamp + fileName.substring(dotIndex);
        } else {
            return fileName + "_" + timestamp;
        }
    }

    private byte[] generatePlaceholderImage(String text) {
        // Generate a simple placeholder image
        // In real implementation, use proper image generation
        String placeholder = "Quote Image: " + text.substring(0, Math.min(text.length(), 50)) + "...";
        return placeholder.getBytes();
    }
}