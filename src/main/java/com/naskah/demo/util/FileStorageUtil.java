package com.naskah.demo.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class FileStorageUtil {

    @Value("${file.upload-directory}")
    private String uploadDir;

    public String saveFile(MultipartFile file, String id) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = id + extension;
        Path destinationFile = uploadPath.resolve(filename).normalize();

        if (!destinationFile.getParent().equals(uploadPath)) {
            throw new RuntimeException("Cannot save the file outside the specified directory.");
        }

        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        return destinationFile.toString();
    }
}