package com.naskah.demo.model.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ============ KIOS PRODUCT RESPONSE ============
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KiosProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String sku;
    private String description;
    private String productType;
    private String category;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal discountPercentage;
    private BigDecimal finalPrice; // Price after discount
    private Integer stockQuantity;
    private Integer minStockLevel;
    private BigDecimal weight;
    private String dimensions;
    private List<String> imageUrls;
    private String thumbnailUrl;
    private Long bookId;
    private String bookTitle;
    private String bookSlug;
    private Boolean isActive;
    private Boolean isFeatured;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
    private Integer viewCount;
    private Integer soldCount;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
