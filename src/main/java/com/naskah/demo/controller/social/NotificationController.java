package com.naskah.demo.controller.social;

import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.dto.response.social.*;
import com.naskah.demo.service.social.NotificationService;
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

    /**
     * GET /api/social/notifications
     * Semua notifikasi saya
     */
    @GetMapping
    public ResponseEntity<DataResponse<NotificationPageResponse>> getMyNotifications(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(notificationService.getMyNotifications(page, limit));
    }

    /**
     * GET /api/social/notifications/unread-count
     * Jumlah notifikasi belum dibaca (untuk badge)
     */
    @GetMapping("/unread-count")
    public ResponseEntity<DataResponse<Integer>> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }

    /**
     * PUT /api/social/notifications/{notificationId}/read
     * Tandai satu notifikasi sudah dibaca
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<DataResponse<Void>> markRead(
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markRead(notificationId));
    }

    /**
     * PUT /api/social/notifications/read-all
     * Tandai semua notifikasi sudah dibaca
     */
    @PutMapping("/read-all")
    public ResponseEntity<DataResponse<Void>> markAllRead() {
        return ResponseEntity.ok(notificationService.markAllRead());
    }
}