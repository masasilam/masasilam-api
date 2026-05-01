package com.naskah.demo.service.social;

import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.dto.response.social.*;

public interface NotificationService {
    DataResponse<NotificationPageResponse> getMyNotifications(int page, int limit);
    DataResponse<Integer> getUnreadCount();
    DataResponse<Void> markRead(Long notificationId);
    DataResponse<Void> markAllRead();
    // Internal
    void sendNotification(Long recipientId, Long senderId, String type,
                          String entityType, Long entityId, String message, String dataJson);
}