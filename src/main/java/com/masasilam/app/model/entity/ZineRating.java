package com.masasilam.app.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZineRating {
    private Long id;
    private Long userId;
    private Long zineId;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}