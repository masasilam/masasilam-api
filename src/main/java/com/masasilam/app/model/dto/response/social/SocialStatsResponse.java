package com.masasilam.app.model.dto.response.social;

import lombok.Data;

@Data
public class SocialStatsResponse {
    private Integer totalActivities;
    private Integer totalLikesGiven;
    private Integer totalLikesReceived;
    private Integer totalCommentsGiven;
    private Integer totalCommentsReceived;
    private Integer totalFollowers;
    private Integer totalFollowing;
    private Integer mutualFollows;
    private Integer totalReadingLists;
    private Integer totalPublicAnnotations;
    private Integer totalGroupsJoined;
    private Integer totalChallengesJoined;
    private Integer totalChallengesCompleted;
}