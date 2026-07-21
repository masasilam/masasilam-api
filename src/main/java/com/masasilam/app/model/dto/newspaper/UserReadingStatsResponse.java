package com.masasilam.app.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReadingStatsResponse {
    private Long userId;
    private Integer totalArticlesRead;
    private Integer totalArticlesSaved;
    private Integer totalReviews;
    private Integer totalReadingTimeMinutes;
    private String favoriteCategory;
    private String favoriteSource;
    private List<String> topTopics;
    private List<NewspaperArticleResponse> recentlyRead;
    private List<SavedArticleResponse> savedArticles;
    private Integer currentStreak;
    private Integer longestStreak;
}