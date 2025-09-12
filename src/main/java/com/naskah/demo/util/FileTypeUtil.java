package com.naskah.demo.util;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class FileTypeUtil {

    private final List<String> imageExtensions = Arrays.asList("jpg", "jpeg", "png");
    private final List<String> documentExtensions = Arrays.asList("pdf", "epub");

    public String detectContentType(String filename) {
        if (filename == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        if (imageExtensions.contains(extension)) {
            return extension.equals("png") ?
                    MediaType.IMAGE_PNG_VALUE :
                    MediaType.IMAGE_JPEG_VALUE;
        } else if (documentExtensions.contains(extension)) {
            return extension.equals("pdf") ?
                    "application/pdf" :
                    "application/epub+zip";
        }

        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}