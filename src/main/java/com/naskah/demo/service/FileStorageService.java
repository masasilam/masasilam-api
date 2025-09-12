//package com.naskah.demo.service;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.UUID;
//
//@Service
//@Slf4j
//public class FileStorageService {
//
//    @Value("${storage.base-path:./storage}")
//    private String basePath;
//
//    @Value("${storage.images.path:./storage/images}")
//    private String imagesPath;
//
//    @Value("${storage.documents.path:./storage/documents}")
//    private String documentsPath;
//
//    public String storeImage(MultipartFile file) throws IOException {
//        return storeFile(file, imagesPath, "image");
//    }
//
//    public String storeDocument(MultipartFile file) throws IOException {
//        return storeFile(file, documentsPath, "document");
//    }
//
//    private String storeFile(MultipartFile file, String storagePath, String type) throws IOException {
//        if (file.isEmpty()) {
//            throw new IOException("Cannot store empty file");
//        }
//
//        // Create directories if they don't exist
//        Path storageDir = Paths.get(storagePath);
//        Files.createDirectories(storageDir);
//
//        // Generate unique filename
//        String originalFilename = file.getOriginalFilename();
//        String extension = originalFilename != null ?
//                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
//
//        String filename = String.format("%s_%s_%s%s",
//                type,
//                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
//                UUID.randomUUID().toString().substring(0, 8),
//                extension
//        );
//
//        Path targetPath = storageDir.resolve(filename);
//
//        // Store file
//        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
//
//        log.info("File stored: {}", targetPath);
//        return targetPath.toString();
//    }
//
//    public void deleteFile(String filePath) {
//        try {
//            Path path = Paths.get(filePath);
//            Files.deleteIfExists(path);
//            log.info("File deleted: {}", filePath);
//        } catch (IOException e) {
//            log.error("Failed to delete file: {}", filePath, e);
//        }
//    }
//}