package com.masasilam.app.model.dto.response.social;

import lombok.Data;
import java.util.List;

@Data
public class ReadingTwinResponse {
    private Long userId;
    private String username;
    private String displayName;
    private String profilePictureUrl;
    private String tagline;
    private Boolean isVerified;
    private Double similarityScore;
    private Integer commonCount;
    private List<CommonContentResponse> commonContent;
    private Boolean isFollowing;

    @Data
    public static class CommonContentResponse {
        private String entityType;
        private Long entityId;
        private String entityTitle;
        private String entityCover;
        private String entitySlug;
    }
}