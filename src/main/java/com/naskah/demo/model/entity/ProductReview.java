package com.naskah.demo.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductReview {
    private Long id;
    private Long userId;
    private Long productId;
    private BigDecimal rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
