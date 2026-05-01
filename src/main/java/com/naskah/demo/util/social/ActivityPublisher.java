// ============================================================
// FILE: util/social/ActivityPublisher.java
// Hook yang dipanggil dari EpubAnnotationServiceImpl,
// BookServiceImpl, ZineServiceImpl, dll saat event penting terjadi.
// Inject ke service yang sudah ada dan panggil method ini.
// ============================================================
package com.naskah.demo.util.social;

import com.naskah.demo.service.social.ActivityFeedService;
import com.naskah.demo.service.social.ReadingChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityPublisher {

    private final ActivityFeedService feedService;
    private final ReadingChallengeService challengeService;

    /**
     * Dipanggil ketika user mulai membaca buku/zine untuk pertama kali
     * Inject ActivityPublisher ke EpubAnnotationServiceImpl
     * dan panggil ini di startReading() ketika existingSessions == 0
     */
    @Async("socialTaskExecutor")
    public void onStartedReading(Long userId, String entityType, Long entityId,
                                 String entitySlug, String entityTitle, String entityCover) {
        try {
            feedService.publishActivity(userId, "started_reading", entityType,
                    entityId, entitySlug, entityTitle, entityCover, "{}", "public");
            log.debug("Published started_reading for user={} entity={}/{}", userId, entityType, entityId);
        } catch (Exception e) {
            log.warn("onStartedReading publish failed: {}", e.getMessage());
        }
    }

    /**
     * Dipanggil ketika reading progress >= 95%
     * Inject ke EpubAnnotationServiceImpl di recordEpubSession()
     * Dipanggil jika newStatus.equals("completed")
     */
    @Async("socialTaskExecutor")
    public void onFinishedReading(Long userId, String entityType, Long entityId,
                                  String entitySlug, String entityTitle, String entityCover) {
        try {
            feedService.publishActivity(userId, "finished_reading", entityType,
                    entityId, entitySlug, entityTitle, entityCover,
                    "{\"completed\":true}", "public");

            // Auto-check and update challenges
            challengeService.checkAndUpdateChallenges(
                    userId, entityType, entityId, entityTitle, entitySlug, entityCover);

            log.debug("Published finished_reading for user={} entity={}/{}", userId, entityType, entityId);
        } catch (Exception e) {
            log.warn("onFinishedReading publish failed: {}", e.getMessage());
        }
    }

    /**
     * Dipanggil ketika user memberikan rating
     * Inject ke BookReactionServiceImpl.addOrUpdateBookRating()
     */
    @Async("socialTaskExecutor")
    public void onRatedContent(Long userId, String entityType, Long entityId,
                               String entitySlug, String entityTitle, String entityCover,
                               Double rating) {
        try {
            String metadata = "{\"rating\":" + rating + "}";
            feedService.publishActivity(userId, "rated_content", entityType,
                    entityId, entitySlug, entityTitle, entityCover, metadata, "public");
        } catch (Exception e) {
            log.warn("onRatedContent publish failed: {}", e.getMessage());
        }
    }

    /**
     * Dipanggil ketika user menulis review
     * Inject ke BookReactionServiceImpl.createBookReview()
     */
    @Async("socialTaskExecutor")
    public void onWroteReview(Long userId, String entityType, Long entityId,
                              String entitySlug, String entityTitle, String entityCover,
                              Long reviewId) {
        try {
            String metadata = "{\"reviewId\":" + reviewId + "}";
            feedService.publishActivity(userId, "wrote_review", entityType,
                    entityId, entitySlug, entityTitle, entityCover, metadata, "public");
        } catch (Exception e) {
            log.warn("onWroteReview publish failed: {}", e.getMessage());
        }
    }

    /**
     * Dipanggil ketika user menambahkan annotation
     * Inject ke EpubAnnotationServiceImpl.addAnnotation()
     */
    @Async("socialTaskExecutor")
    public void onAddedAnnotation(Long userId, String entityType, Long entityId,
                                  String entitySlug, String entityTitle, Long annotationId) {
        try {
            String metadata = "{\"annotationId\":" + annotationId + "}";
            feedService.publishActivity(userId, "added_annotation", entityType,
                    entityId, entitySlug, entityTitle, null, metadata, "followers");
        } catch (Exception e) {
            log.warn("onAddedAnnotation publish failed: {}", e.getMessage());
        }
    }
}