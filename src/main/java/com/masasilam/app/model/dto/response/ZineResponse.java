package com.masasilam.app.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZineResponse {
    private Long id;
    private String title;
    private String slug;
    private String subtitle;
    private Integer volume;
    private String issueNumber;
    private Integer publicationYear;
    private String publisher;
    private String languageName;
    private String description;
    private String fileUrl;
    private String coverImageUrl;
    private String fileFormat;
    private Long fileSize;
    private String copyrightStatus;
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
    private Double averageRating;
    private Long totalRatings;
    private List<AuthorResponse> authors;
    private List<ContributorResponse> contributors;
    private List<GenreResponse> genres;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String firstPublisher;
    private String firstPublishedDate;
    private String collectionName;
}