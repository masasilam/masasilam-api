package com.naskah.app.model.dto.response.social;

import lombok.Data;
import java.util.List;

@Data
public class FeedPageResponse {
    private List<ActivityFeedItemResponse> items;
    private Integer total;
    private Integer page;
    private Integer limit;
    private Boolean hasMore;
}