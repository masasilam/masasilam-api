package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookResponse {
    private Long id;
    private String title;
    private String slug;
    private String coverImageUrl;
    private String description;
    private String summary;
    private Integer estimatedReadTime;
    private String language;
    private Long totalWord;
    private Integer difficultyLevel;
    private Integer totalPages;
    private String publisher;
    private Integer publicationYear;
    private String copyrightStatus;
    private LocalDateTime publishedAt;
    private Integer edition;
    private String fileUrl;
    private String fileFormat;
    private Long fileSize;
    private Integer viewCount;
    private Integer readCount;
    private Integer downloadCount;
    private String subtitle;
    private Integer seriesId;
    private Integer seriesOrder;
    private String category;

    // Aggregated string fields from database
    private String authorNames;
    private String authorSlugs;
    private String contributors;
    private String genres;

    // Reaction statistics
    private Integer totalRatings;
    private Integer totalAngry;
    private Integer totalLikes;
    private Integer totalLoves;
    private Integer totalDislikes;
    private Integer totalSad;
    private Integer totalComments;
    private Integer totalReactions;
    private Double averageRating;
}