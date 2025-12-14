package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookRating {
    private Long id;
    private Long userId;
    private Long bookId;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}