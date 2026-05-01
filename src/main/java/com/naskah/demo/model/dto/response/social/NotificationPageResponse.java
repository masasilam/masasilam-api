package com.naskah.demo.model.dto.response.social;

import lombok.Data;
import java.util.List;

@Data
public class NotificationPageResponse {
    private List<NotificationResponse> items;
    private Integer total;
    private Integer unreadCount;
    private Integer page;
    private Integer limit;
}