package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FilmReviewReplyResponse {
    private Long          id;
    private Long          userId;
    private String        username;
    private String        userPhotoUrl;
    private Long          reviewId;
    private Long          parentReplyId;
    private String        content;
    private Boolean       isOwner;         // true jika current user adalah pemilik reply
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}