//package com.naskah.demo.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.skah.digitallibrary.model.Notification;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class WebSocketService {
//
//    private final SimpMessagingTemplate messagingTemplate;
//    private final ObjectMapper objectMapper;
//
//    public void sendNotificationToUser(Long userId, Notification notification) {
//        try {
//            String destination = "/topic/user/" + userId + "/notifications";
//            messagingTemplate.convertAndSend(destination, notification);
//            log.debug("Real-time notification sent to user {} via WebSocket", userId);
//        } catch (Exception e) {
//            log.error("Failed to send WebSocket notification to user {}", userId, e);
//        }
//    }
//
//    public void sendProjectUpdate(Long projectId, Object updateData) {
//        try {
//            String destination = "/topic/project/" + projectId + "/updates";
//            messagingTemplate.convertAndSend(destination, updateData);
//        } catch (Exception e) {
//            log.error("Failed to send project update via WebSocket for project {}", projectId, e);
//        }
//    }
//
//    public void sendGlobalAnnouncement(Object announcement) {
//        try {
//            messagingTemplate.convertAndSend("/topic/announcements", announcement);
//        } catch (Exception e) {
//            log.error("Failed to send global announcement via WebSocket", e);
//        }
//    }
//}
//
