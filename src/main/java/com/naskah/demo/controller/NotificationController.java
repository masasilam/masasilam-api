//package com.naskah.demo.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.skah.digitallibrary.dto.ApiResponse;
//import org.skah.digitallibrary.model.Notification;
//import org.skah.digitallibrary.model.User;
//import org.skah.digitallibrary.service.NotificationService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/notifications")
//@RequiredArgsConstructor
//public class NotificationController {
//
//    private final NotificationService notificationService;
//
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
//            @AuthenticationPrincipal User currentUser) {
//        List<Notification> notifications = notificationService.findByUserId(currentUser.getId());
//        return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications retrieved successfully"));
//    }
//
//    @PostMapping("/{id}/read")
//    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
//        try {
//            notificationService.markAsRead(id);
//            return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//}