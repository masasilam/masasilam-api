package com.naskah.demo.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Book {
    private Long id;
    private String title;
    private String slug;
    private String subtitle;
    private Integer seriesId;
    private Integer seriesOrder;
    private String isbn;
    private Integer publicationYear;
    private String publisher;
    private Integer languageId;
    private String description;
    private String summary;
    private String coverImageUrl;
    private String coverImagePath;
    private String fileUrl;
    private String filePath;
    private String fileFormat;
    private Long fileSize;
    private Long totalWord;
    private Integer totalPages;
    private Integer estimatedReadTime;
    private Integer difficultyLevel;
    private Integer copyrightStatusId;
    private Integer viewCount;
    private Integer readCount;
    private Integer downloadCount;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Boolean isFeatured;
    private Boolean isActive;
    private LocalDateTime publishedAt;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
