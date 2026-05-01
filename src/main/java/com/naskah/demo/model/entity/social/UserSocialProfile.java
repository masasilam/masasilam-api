package com.naskah.demo.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class UserSocialProfile {
    private Long id;
    private Long userId;
    private String displayName;
    private String tagline;
    private String location;
    private String websiteUrl;
    private String socialLinks;
    private String readingVisibility;
    private String annotationVisibility;
    private String profileTheme;
    private Integer totalFollowers;
    private Integer totalFollowing;
    private Boolean isVerified;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}