package com.naskah.app.model.dto.response.social;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FollowerItemResponse {
    private Long userId;
    private String username;
    private String displayName;
    private String profilePictureUrl;
    private String tagline;
    private Boolean isVerified;
    private Boolean isFollowing; // does current user follow them
    private OffsetDateTime followedAt;
}