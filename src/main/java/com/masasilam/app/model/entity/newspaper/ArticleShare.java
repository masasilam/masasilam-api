package com.masasilam.app.model.entity.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleShare {
    private Long id;
    private Long articleId;
    private Long userId;
    private String platform;
    private LocalDateTime createdAt;
}
