package com.masasilam.app.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Zine {
    private Long id;
    private String title;
    private String slug;
    private String subtitle;
    private Integer volume;
    private String issueNumber;
    private Integer publicationYear;
    private String publisher;
    private Long languageId;
    private String description;
    private String fileUrl;
    private String fileUrlArchive;
    private String filePath;
    private String coverImageUrl;
    private String fileFormat;
    private Long fileSize;
    private Long copyrightStatusId;
    private Integer viewCount;
    private Integer readCount;
    private Integer downloadCount;
    private Integer totalWord;
    private Integer totalPages;
    private Integer estimatedReadTime;
    private Boolean isActive;
    private Boolean isFeatured;
    private OffsetDateTime publishedAt;
    private String category;
    private String source;
    private String difficultyLevel;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    // Tambahkan field berikut
    private String  firstPublisher;       // TAMBAH
    private String  firstPublishedDate;   // TAMBAH — "1955-02"
    private String  collectionName;       // TAMBAH — "Zaman Baru"
}