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
public class ZineReview {
    private Long id;
    private Long userId;
    private Long zineId;
    private String title;
    private String content;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private Integer replyCount;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}