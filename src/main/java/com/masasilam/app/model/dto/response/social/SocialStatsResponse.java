package com.masasilam.app.model.dto.response.social;

import lombok.Data;

@Data
public class SocialStatsResponse {
    // Feed
    private Integer totalActivities;
    private Integer totalLikesGiven;
    private Integer totalLikesReceived;
    private Integer totalCommentsGiven;
    private Integer totalCommentsReceived;

    // Social graph
    private Integer totalFollowers;
    private Integer totalFollowing;
    private Integer mutualFollows;

    // Content
    private Integer totalReadingLists;
    private Integer totalPublicAnnotations;
    private Integer totalGroupsJoined;
    private Integer totalChallengesJoined;
    private Integer totalChallengesCompleted;
}