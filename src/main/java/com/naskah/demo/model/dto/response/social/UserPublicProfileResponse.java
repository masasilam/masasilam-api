package com.naskah.demo.model.dto.response.social;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class UserPublicProfileResponse {
    private Long userId;
    private String username;
    private String displayName;
    private String profilePictureUrl;
    private String tagline;
    private String location;
    private String websiteUrl;
    private String socialLinks;
    private String readingVisibility;
    private String profileTheme;
    private Boolean isVerified;

    // Stats
    private Integer totalFollowers;
    private Integer totalFollowing;
    private Integer totalBooksRead;
    private Integer readingStreakDays;
    private Integer contributedBooksCount;
    private Double averageRating;
    private Integer experiencePoints;
    private String level;

    private Integer followerCount;
    private Integer followingCount;
    private Integer totalActivities;
    private Integer totalReadingLists;

    // Current viewer context
    private Boolean isFollowing;
    private Boolean isFollowedBy;

    // Recent activity previews
    private List<ActivityFeedItemResponse> recentActivities;
    private List<ReadingListSummaryResponse> publicLists;
    private List<SocialAnnotationResponse> publicAnnotations;

    private LocalDateTime memberSince;
}