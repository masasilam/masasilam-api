package com.naskah.demo.model.dto.response.social;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class GroupMemberResponse {
    private Long userId;
    private String username;
    private String displayName;
    private String profilePictureUrl;
    private String role;
    private Boolean isVerified;
    private OffsetDateTime joinedAt;
}