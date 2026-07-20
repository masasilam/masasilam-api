package com.masasilam.app.service.social.impl;

import com.masasilam.app.exception.custom.*;
import com.masasilam.app.mapper.UserMapper;
import com.masasilam.app.mapper.social.SocialNotificationMapper;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.model.dto.response.social.NotificationResponse;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.entity.social.SocialNotification;
import com.masasilam.app.service.social.NotificationService;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final SocialNotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";
    private static final int MAX_NOTIFICATIONS = 200;

    private User requireAuth() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    @Override
    public DataResponse<NotificationPageResponse> getMyNotifications(int page, int limit) {
        User me = requireAuth();
        int offset = (page - 1) * limit;
        List<NotificationResponse> items = notificationMapper.findByRecipient(me.getId(), offset, limit);
        items.forEach(n -> n.setTimeAgo(timeAgo(n.getCreatedAt().toLocalDateTime())));

        NotificationPageResponse response = new NotificationPageResponse();
        response.setItems(items);
        response.setTotal(notificationMapper.countAll(me.getId()));
        response.setUnreadCount(notificationMapper.countUnread(me.getId()));
        response.setPage(page);
        response.setLimit(limit);
        return new DataResponse<>(SUCCESS, "Notifications retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    public DataResponse<Integer> getUnreadCount() {
        User me = requireAuth();
        int count = notificationMapper.countUnread(me.getId());
        return new DataResponse<>(SUCCESS, "Unread count", HttpStatus.OK.value(), count);
    }

    @Override
    public DataResponse<Void> markRead(Long notificationId) {
        requireAuth();
        notificationMapper.markRead(notificationId);
        return new DataResponse<>(SUCCESS, "Marked as read", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<Void> markAllRead() {
        User me = requireAuth();
        notificationMapper.markAllRead(me.getId());
        return new DataResponse<>(SUCCESS, "All marked as read", HttpStatus.OK.value(), null);
    }

    @Override
    @Async
    public void sendNotification(Long recipientId, Long senderId, String type,
                                 String entityType, Long entityId,
                                 String message, String dataJson) {
        try {
            if (recipientId == null || recipientId.equals(senderId)) return;

            SocialNotification notification = new SocialNotification();
            notification.setRecipientId(recipientId);
            notification.setSenderId(senderId);
            notification.setType(type);
            notification.setEntityType(entityType);
            notification.setEntityId(entityId);
            notification.setMessage(message);
            notification.setData(dataJson != null ? dataJson : "{}");
            notificationMapper.insert(notification);

            notificationMapper.deleteOld(recipientId, MAX_NOTIFICATIONS);
        } catch (Exception e) {
            log.warn("sendNotification failed for recipient={}: {}", recipientId, e.getMessage());
        }
    }

    private String timeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        Duration d = Duration.between(dateTime, LocalDateTime.now());
        if (d.toMinutes() < 1) return "baru saja";
        if (d.toMinutes() < 60) return d.toMinutes() + " menit lalu";
        if (d.toHours() < 24) return d.toHours() + " jam lalu";
        if (d.toDays() < 7) return d.toDays() + " hari lalu";
        if (d.toDays() < 30) return (d.toDays() / 7) + " minggu lalu";
        return (d.toDays() / 30) + " bulan lalu";
    }
}