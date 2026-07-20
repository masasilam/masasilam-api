package com.masasilam.app.model.dto.newspaper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperArticleDetailResponse {
    private Long id;
    private String slug;

    // ✅ FIX SOURCE: field "source" adalah nested object yang dibangun manual di service
    // Diisi oleh enrichArticleDetailResponse() dari flat fields di bawah
    private NewspaperSourceResponse source;

    // ✅ Flat source fields — diisi langsung oleh MyBatis dari query SQL alias
    // (ns.id as sourceId, ns.name as sourceName, dst)
    // JsonIgnore agar tidak duplikat di JSON response (sudah ada di "source")
    @JsonIgnore
    private Long sourceId;
    @JsonIgnore
    private String sourceName;
    @JsonIgnore
    private String sourceLocation;
    @JsonIgnore
    private String sourceDescription;

    // Category & Date
    private String category;
    private String categoryName;
    private LocalDate publishDate;
    private String dateFormatted;

    // Content
    private String title;
    private String subtitle;
    private String bodyOriginal;   // mapped dari html_content
    private String bodyModern;
    private String excerpt;

    // Metadata
    private String author;
    private Integer pageNumber;
    private Integer columnNumber;
    private String importance;
    private Integer wordCount;
    private String imageUrl;

    // Hierarchy
    private Long parentArticleId;
    private Integer articleLevel;

    // Statistics
    private Long viewCount;
    private Long readCount;
    private Integer shareCount;
    private Integer saveCount;
    private Integer commentCount;
    private BigDecimal averageRating;
    private Integer ratingCount;

    // User-specific data
    private Boolean isSaved;
    private Double myRating;
    private Boolean hasReviewed;

    // Related content
    private List<NewspaperArticleResponse> relatedArticles;
    private List<NewspaperArticleResponse> sameDateArticles;

    // Tags
    private List<String> tags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}