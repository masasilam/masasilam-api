package com.masasilam.app.mapper.collaboration;

import com.masasilam.app.model.dto.response.NotificationResponse;
import com.masasilam.app.model.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    List<NotificationResponse> getUserNotifications(@Param("userId") Long userId, @Param("i") int i, @Param("i1") int i1, @Param("desc") String desc);
    void insertNotification(Notification notification);
}
