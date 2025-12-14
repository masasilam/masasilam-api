package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookRatingResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userPhotoUrl;
    private Long bookId;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}