package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class BlogStatsResponse {
    private Long totalPosts;
    private Long totalViews;
    private Long totalLikes;
    private Long totalComments;
    private Long publishedPosts;
    private Long draftPosts;

    // Monthly statistics
    private List<MonthlyStatsResponse> monthlyStats;
}
