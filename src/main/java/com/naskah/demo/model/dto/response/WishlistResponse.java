package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============ WISHLIST RESPONSE ============
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WishlistResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private String thumbnailUrl;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Boolean isAvailable;
    private Boolean isInStock;
    private LocalDateTime addedAt;
}
