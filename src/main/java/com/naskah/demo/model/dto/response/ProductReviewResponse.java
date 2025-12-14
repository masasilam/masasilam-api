package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============ PRODUCT REVIEW RESPONSE ============
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductReviewResponse {
    private Long id;
    private Long userId;
    private String username;
    private String userAvatar;
    private Long productId;
    private String productName;
    private BigDecimal rating;
    private String comment;
    private Boolean isVerifiedPurchase;
    private Integer helpfulCount;
    private Boolean isHelpful; // For current user
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
