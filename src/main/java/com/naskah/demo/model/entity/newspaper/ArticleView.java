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
public class ArticleView {
    private Long id;
    private Long articleId;
    private Long userId; // NULL for guests
    private String ipAddress;
    private String userAgent;
    private String viewerHash;
    private String actionType; // view, read, share
    private LocalDateTime createdAt;
}
