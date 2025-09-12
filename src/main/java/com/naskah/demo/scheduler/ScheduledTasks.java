//package com.naskah.demo.scheduler;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import java.time.LocalDateTime;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class ScheduledTasks {
//
//    private final ProjectService projectService;
//    private final UserService userService;
//    private final NotificationService notificationService;
//
//    @Scheduled(cron = "${scheduler.deadline-reminder.cron:0 0 9 * * ?}")
//    public void sendDeadlineReminders() {
//        log.info("Running deadline reminder task");
//
//        // Find projects with deadlines in next 3 days
//        LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);
//
//        // Implementation would involve querying projects with upcoming deadlines
//        // and sending notifications to assigned users
//
//        log.info("Deadline reminder task completed");
//    }
//
//    @Scheduled(cron = "${scheduler.cleanup.cron:0 0 2 * * ?}")
//    public void cleanupOldNotifications() {
//        log.info("Running cleanup task");
//
//        // Clean up old notifications (older than 30 days)
//        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
//
//        // Implementation would involve deleting old notifications
//        // and performing other cleanup tasks
//
//        log.info("Cleanup task completed");
//    }
//
//    @Scheduled(fixedRate = 300000) // Every 5 minutes
//    public void releaseStaleCheckouts() {
//        log.info("Checking for stale page checkouts");
//
//        // Release pages that have been checked out for too long
//        // Implementation would check checkout timeout and release pages
//
//        log.info("Stale checkout check completed");
//    }
//}
