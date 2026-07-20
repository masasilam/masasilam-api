package com.masasilam.app.controller.social;

import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.service.social.NotificationService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/social/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<DataResponse<NotificationPageResponse>> getMyNotifications(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                                     @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(notificationService.getMyNotifications(page, limit));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<DataResponse<Integer>> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<DataResponse<Void>> markRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markRead(notificationId));
    }

    @PutMapping("/read-all")
    public ResponseEntity<DataResponse<Void>> markAllRead() {
        return ResponseEntity.ok(notificationService.markAllRead());
    }
}