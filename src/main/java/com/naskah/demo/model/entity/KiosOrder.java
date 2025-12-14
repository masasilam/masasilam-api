package com.naskah.demo.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class KiosOrder {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String status; // PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus; // UNPAID, PAID, REFUNDED
    private LocalDateTime paidAt;
    private String shippingAddress;
    private String shippingMethod;
    private String shippingTrackingNumber;
    private String shippingCourier;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private String notes;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}