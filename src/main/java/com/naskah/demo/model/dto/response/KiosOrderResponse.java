package com.naskah.demo.model.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ============ KIOS ORDER RESPONSE ============
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KiosOrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String username;
    private String userEmail;
    private String status;
    private String statusLabel;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime paidAt;
    private ShippingInfo shippingInfo;
    private String notes;
    private String adminNotes;
    private List<OrderItemResponse> items;
    private Integer totalItems;
    private Integer totalQuantity;
    private Boolean canCancel;
    private Boolean canPay;
    private Boolean canReview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productSlug;
        private String thumbnailUrl;
        private String productType;
        private String sku;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
        private Boolean hasReviewed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingInfo {
        private String recipientName;
        private String phoneNumber;
        private String fullAddress;
        private String method;
        private String trackingNumber;
        private String courier;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;
        private LocalDateTime estimatedDelivery;
    }
}
