package com.naskah.demo.model.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============ KIOS PRODUCT ============
@Data
public class KiosProduct {
    private Long id;
    private String name;
    private String slug;
    private String sku;
    private String description;
    private String productType; // PHYSICAL_BOOK, ACCESSORY, MERCHANDISE
    private String category;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal discountPercentage;
    private Integer stockQuantity;
    private Integer minStockLevel;
    private BigDecimal weight; // in grams
    private String dimensions; // LxWxH in cm
    private String imageUrls; // comma-separated URLs
    private String thumbnailUrl;
    private Long bookId; // Reference to book if product is physical book
    private Boolean isActive;
    private Boolean isFeatured;
    private Integer viewCount;
    private Integer soldCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}