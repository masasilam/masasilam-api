package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Book {
    private Long id;
    private String title;
    private String slug;
    private String subtitle;
    private Long seriesId;
    private Integer seriesOrder;
    private Integer edition;
    private Integer publicationYear;
    private String publisher;
    private String description;
    private String summary;
    private String source;
    private String coverImageUrl;
    private String fileUrl;
    private String fileFormat;
    private Long fileSize;
    private Integer totalPages;
    private Long totalWord;
    private Integer estimatedReadTime;
    private String difficultyLevel;
    private Integer viewCount;
    private Integer readCount;
    private Integer downloadCount;
    private Boolean isFeatured;
    private Boolean isActive;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer languageId;
    private Integer copyrightStatusId;
    private String filePath;
    private String coverImagePath;
    private String category;
}
