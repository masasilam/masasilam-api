package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockHistory {
    private Long id;
    private Long productId;
    private Integer quantityChange;
    private String type; // INITIAL, ORDER, RESTOCK, ADJUSTMENT, RETURN, CANCELLED
    private String reason;
    private String orderNumber;
    private String createdBy;
    private LocalDateTime createdAt;
}
