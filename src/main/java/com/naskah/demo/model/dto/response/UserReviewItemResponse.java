package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserReviewItemResponse {
    private Long reviewId;
    private Long bookId;
    private String bookTitle;
    private String bookSlug;
    private String bookCover;

    private String reviewTitle;
    private String reviewContent;
    private Double rating;

    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private Integer replyCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
