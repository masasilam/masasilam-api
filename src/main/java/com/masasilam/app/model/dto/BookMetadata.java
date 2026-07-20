package com.masasilam.app.model.dto;

import lombok.Data;

@Data
public class BookMetadata {
    private final String fileFormat;
    private final long fileSize;
    private final int totalPages;
    private final long totalWord;
}