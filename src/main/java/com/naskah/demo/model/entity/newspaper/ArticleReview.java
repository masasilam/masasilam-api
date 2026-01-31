package com.naskah.demo.model.entity.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleReview {
    private Long id;
    private Long userId;
    private Long articleId;
    private String title;
    private String content;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private Integer replyCount;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
