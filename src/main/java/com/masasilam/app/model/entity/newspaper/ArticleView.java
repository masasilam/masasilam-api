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
public class ArticleView {
    private Long id;
    private Long articleId;
    private Long userId;
    private String ipAddress;
    private String userAgent;
    private String viewerHash;
    private String actionType;
    private LocalDateTime createdAt;
}
