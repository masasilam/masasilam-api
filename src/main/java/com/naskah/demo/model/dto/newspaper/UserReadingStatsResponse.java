package com.naskah.demo.model.dto.newspaper;

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

    // Reading stats
    private Integer totalArticlesRead;
    private Integer totalArticlesSaved;
    private Integer totalReviews;
    private Integer totalReadingTimeMinutes;

    // Preferences
    private String favoriteCategory;
    private String favoriteSource;
    private List<String> topTopics;

    // Recent activity
    private List<NewspaperArticleResponse> recentlyRead;
    private List<SavedArticleResponse> savedArticles;

    // Streaks
    private Integer currentStreak;
    private Integer longestStreak;
}
