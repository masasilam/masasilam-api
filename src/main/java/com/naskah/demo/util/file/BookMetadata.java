package com.naskah.demo.util.file;

import lombok.Data;

@Data
public class BookMetadata {
    private final String fileFormat;
    private final long fileSize;
    private final int totalPages;
    private final long totalWord;
    private final int difficultyLevel;
}