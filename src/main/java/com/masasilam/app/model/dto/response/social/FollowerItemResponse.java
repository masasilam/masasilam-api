package com.masasilam.app.model.dto.response.social;

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
    private Boolean isFollowing;
    private OffsetDateTime followedAt;
}