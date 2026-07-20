package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZineRatingResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userPhotoUrl;
    private Long zineId;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}