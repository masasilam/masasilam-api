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
    private List<AuthorResponse> authors;
    private List<ContributorResponse> contributors;
    private List<GenreResponse> genres;
    private Integer viewCount;
    private Integer readCount;
    private Integer downloadCount;
    private List<ReactionStatsResponse> reactionStatsResponses;
    private String subtitle;
    private Integer seriesId;
    private Integer seriesOrder;
    private String category;
}