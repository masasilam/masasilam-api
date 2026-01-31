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
public class SavedArticle {
    private Long id;
    private Long userId;
    private Long articleId;
    private String collectionName;
    private String notes;
    private LocalDateTime createdAt;
}
