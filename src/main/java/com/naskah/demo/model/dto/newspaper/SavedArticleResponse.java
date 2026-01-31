package com.naskah.demo.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedArticleResponse {
    private Long id;
    private Long articleId;
    private String collectionName;
    private String notes;
    private NewspaperArticleResponse article; // Full article data
    private LocalDateTime savedAt;
}
