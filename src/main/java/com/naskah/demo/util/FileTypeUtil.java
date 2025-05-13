package com.naskah.demo.util;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class FileTypeUtil {
    public String contentType(String filename) {
        if (filename == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        String lowerCaseFilename = filename.toLowerCase();
        if (lowerCaseFilename.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerCaseFilename.endsWith(".epub")) {
            return "application/epub+zip";
        } else if (lowerCaseFilename.endsWith(".txt")) {
            return MediaType.TEXT_PLAIN_VALUE;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}