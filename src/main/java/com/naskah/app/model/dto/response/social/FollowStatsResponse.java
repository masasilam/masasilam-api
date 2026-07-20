package com.naskah.app.model.dto.response.social;

import lombok.Data;
import java.util.List;

@Data
public class FollowStatsResponse {
    private Long userId;
    private Integer totalFollowers;
    private Integer totalFollowing;
    private List<FollowerItemResponse> followers;
    private List<FollowerItemResponse> following;
}