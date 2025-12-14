package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ============ CART RESPONSE ============
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {
    private List<CartItemResponse> items;
    private Integer totalItems;
    private Integer totalQuantity;
    private BigDecimal subtotal;
    private BigDecimal estimatedShipping;
    private BigDecimal totalAmount;
    private Boolean hasUnavailableItems;
    private List<String> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productSlug;
        private String thumbnailUrl;
        private String productType;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private Integer quantity;
        private BigDecimal subtotal;
        private Integer stockAvailable;
        private Boolean isAvailable;
        private String unavailableReason;
        private LocalDateTime addedAt;
    }
}
