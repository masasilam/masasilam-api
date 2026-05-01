// ============================================================
// FILE: config/SchedulingConfig.java
// (Scheduled job untuk kalkulasi Reading Twins)
// ============================================================
package com.naskah.demo.config;

import com.naskah.demo.mapper.UserMapper;
import com.naskah.demo.model.entity.User;
import com.naskah.demo.service.social.ReadingTwinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {

    private final ReadingTwinService twinService;
    private final UserMapper userMapper;

    /**
     * Kalkulasi ulang Reading Twins setiap malam jam 02:00
     * Hanya untuk user aktif (yang memiliki reading session dalam 30 hari terakhir)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void recalculateReadingTwins() {
        log.info("Starting scheduled Reading Twin calculation...");
        try {
            List<Long> activeUserIds = userMapper.findActiveUserIds(30); // active in last 30 days
            log.info("Calculating twins for {} active users", activeUserIds.size());
            activeUserIds.forEach(twinService::calculateTwinsForUser);
            log.info("Reading Twin calculation complete");
        } catch (Exception e) {
            log.error("Reading Twin scheduled job failed: {}", e.getMessage(), e);
        }
    }
}