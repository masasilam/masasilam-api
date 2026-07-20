package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class UserBookStatistics {
    private Integer totalBookmarks;
    private Integer totalHighlights;
    private Integer totalNotes;
    private Integer totalRatings;
    private Integer totalSearches;
    private Integer totalReadingSessions;
    private Double engagementScore; // 0-100
}
