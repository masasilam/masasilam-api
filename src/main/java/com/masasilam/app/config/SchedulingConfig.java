package com.masasilam.app.config;

import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.service.social.ReadingTwinService;
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