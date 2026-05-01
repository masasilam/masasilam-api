package com.naskah.demo.mapper.social;

import com.naskah.demo.model.entity.social.SocialNotification;
import com.naskah.demo.model.dto.response.social.NotificationResponse;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SocialNotificationMapper {
    void insert(SocialNotification notification);
    void markRead(Long id);
    void markAllRead(Long recipientId);
    void deleteOld(@Param("recipientId") Long recipientId, @Param("keepCount") int keepCount);

    List<NotificationResponse> findByRecipient(@Param("recipientId") Long recipientId,
                                               @Param("offset") int offset, @Param("limit") int limit);
    int countUnread(Long recipientId);
    int countAll(Long recipientId);
}