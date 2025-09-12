package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.naskah.demo.model.entity.Tag;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookResponse {
    private Long id;
    private String title;
    private String slug;
    private String isbn;
    private String coverImageUrl;
    private String publisher;
    private Integer publicationYear;
    private String fileUrl;
    private String fileFormat;
    private Long fileSize;
    private Integer totalPages;
    private String description;
    private String summary;
    private Integer estimatedReadTime;
    private String language;
    private Long totalWord;
    private Integer difficultyLevel;
    private List<AuthorResponse> authors;
    private List<GenreResponse> genres;
    private List<Tag> tags;
    private Integer viewCount;
    private Integer readCount;
    private Integer downloadCount;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private String subtitle;
    private Integer seriesId;
    private Integer seriesOrder;
    private String copyrightStatus;
    private LocalDateTime publishedAt;
    private String category;
}
