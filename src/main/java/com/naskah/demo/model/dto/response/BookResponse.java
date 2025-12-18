package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookResponse {
    private Long id;
    private String title;
    private String slug;
    private String subtitle;
    private Integer edition;
    private String coverImageUrl;
    private String publisher;
    private Integer publicationYear;
    private String fileUrl;
    private String fileFormat;
    private Long fileSize;
    private Integer totalPages;
    private String description;
    private Integer estimatedReadTime;
    private String language;
    private Long totalWord;
    private String difficultyLevel;
    private Integer viewCount;
    private Integer readCount;
    private Integer downloadCount;
    private Long seriesId;
    private Integer seriesOrder;
    private String copyrightStatus;
    private LocalDateTime publishedAt;
    private String category;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private Boolean isFeatured;
    private String authorNames;
    private String authorSlugs;
    private String contributors;
    private String genres;
    private Integer totalRatings;
    private Integer totalAngry;
    private Integer totalLikes;
    private Integer totalLoves;
    private Integer totalDislikes;
    private Integer totalSad;
    private Integer totalComments;
    private Double averageRating;
    private Double totalReactions;
}