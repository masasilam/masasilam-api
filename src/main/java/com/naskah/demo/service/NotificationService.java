//package com.naskah.demo.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//@Slf4j
//public class NotificationService {
//
//    private final NotificationMapper notificationMapper;
//    private final EmailService emailService;
//    private final WhatsAppService whatsAppService;
//    private final WebSocketService webSocketService;
//
//    public void sendWelcomeNotification(User user) {
//        Notification notification = new Notification();
//        notification.setUserId(user.getId());
//        notification.setType(NotificationType.SYSTEM_ANNOUNCEMENT);
//        notification.setTitle("Selamat Datang di Skah.org!");
//        notification.setMessage("Terima kasih telah bergabung sebagai relawan. Mari mulai berkontribusi!");
//        notification.setActionUrl("/dashboard");
//        notification.setIsRead(false);
//
//        createNotification(notification);
//        sendNotificationToUser(user, notification);
//    }
//
//    public void sendLevelUpNotification(User user, UserLevel newLevel) {
//        Notification notification = new Notification();
//        notification.setUserId(user.getId());
//        notification.setType(NotificationType.LEVEL_UP);
//        notification.setTitle("Selamat! Level Naik!");
//        notification.setMessage(String.format("Anda telah naik ke level %s: %s", newLevel.name(), newLevel.getDescription()));
//        notification.setActionUrl("/profile");
//        notification.setIsRead(false);
//
//        createNotification(notification);
//        sendNotificationToUser(user, notification);
//    }
//
//    public void sendPageCheckedOutNotification(User user, ProjectPage page) {
//        Notification notification = new Notification();
//        notification.setUserId(user.getId());
//        notification.setType(NotificationType.PAGE_ASSIGNED);
//        notification.setTitle("Halaman Baru Tersedia");
//        notification.setMessage(String.format("Halaman %d dari proyek telah diberikan kepada Anda", page.getPageNumber()));
//        notification.setActionUrl(String.format("/pages/%d", page.getId()));
//        notification.setIsRead(false);
//
//        createNotification(notification);
//        sendNotificationToUser(user, notification);
//    }
//
//    public void sendPageCompletedNotification(User user, ProjectPage page) {
//        Notification notification = new Notification();
//        notification.setUserId(user.getId());
//        notification.setType(NotificationType.PROJECT_COMPLETED);
//        notification.setTitle("Halaman Selesai!");
//        notification.setMessage(String.format("Terima kasih telah menyelesaikan halaman %d. +10 poin pengalaman!", page.getPageNumber()));
//        notification.setActionUrl("/dashboard");
//        notification.setIsRead(false);
//
//        createNotification(notification);
//        sendNotificationToUser(user, notification);
//    }
//
//    public void sendProjectCreatedNotification(Project project) {
//        // Notify all eligible volunteers
//        List<User> volunteers = getEligibleVolunteers(project.getDifficulty());
//
//        for (User volunteer : volunteers) {
//            Notification notification = new Notification();
//            notification.setUserId(volunteer.getId());
//            notification.setType(NotificationType.PROJECT_ASSIGNED);
//            notification.setTitle("Proyek Baru Tersedia");
//            notification.setMessage(String.format("Proyek baru '%s' tersedia untuk dikerjakan", project.getTitle()));
//            notification.setActionUrl(String.format("/projects/%d", project.getId()));
//            notification.setIsRead(false);
//
//            createNotification(notification);
//            sendNotificationToUser(volunteer, notification);
//        }
//    }
//
//    public void sendProjectStageAdvancedNotification(Project project, ProjectStatus newStatus) {
//        log.info("Project {} advanced to stage: {}", project.getId(), newStatus);
//        // Implementation for stage advancement notifications
//    }
//
//    public void sendProjectPublishedNotification(Project project) {
//        log.info("Project {} has been published", project.getId());
//        // Implementation for published project notifications
//    }
//
//    public void sendFeedbackNotification(User user, String feedback) {
//        Notification notification = new Notification();
//        notification.setUserId(user.getId());
//        notification.setType(NotificationType.FEEDBACK_RECEIVED);
//        notification.setTitle("Feedback Baru");
//        notification.setMessage(feedback);
//        notification.setActionUrl("/feedback");
//        notification.setIsRead(false);
//
//        createNotification(notification);
//        sendNotificationToUser(user, notification);
//    }
//
//    public void sendDeadlineReminder(User user, Project project) {
//        Notification notification = new Notification();
//        notification.setUserId(user.getId());
//        notification.setType(NotificationType.DEADLINE_REMINDER);
//        notification.setTitle("Pengingat Deadline");
//        notification.setMessage(String.format("Proyek '%s' mendekati deadline", project.getTitle()));
//        notification.setActionUrl(String.format("/projects/%d", project.getId()));
//        notification.setIsRead(false);
//
//        createNotification(notification);
//        sendNotificationToUser(user, notification);
//    }
//
//    private void createNotification(Notification notification) {
//        notification.setCreatedAt(LocalDateTime.now());
//        notification.setEmailSent(false);
//        notification.setWhatsappSent(false);
//        notificationMapper.insert(notification);
//    }
//
//    private void sendNotificationToUser(User user, Notification notification) {
//        // Send real-time notification via WebSocket
//        webSocketService.sendNotificationToUser(user.getId(), notification);
//
//        // Send email if user has email notifications enabled
//        if (user.getEmailNotifications() && !notification.getEmailSent()) {
//            emailService.sendNotificationEmail(user, notification);
//            notificationMapper.markEmailSent(notification.getId());
//        }
//
//        // Send WhatsApp if user has WhatsApp notifications enabled
//        if (user.getWhatsappNotifications() && !notification.getWhatsappSent()) {
//            whatsAppService.sendNotificationWhatsApp(user, notification);
//            notificationMapper.markWhatsAppSent(notification.getId());
//        }
//    }
//
//    private List<User> getEligibleVolunteers(ProjectDifficulty difficulty) {
//        // Implementation to get eligible volunteers based on difficulty
//        return List.of(); // Placeholder
//    }
//
//    public List<Notification> findByUserId(Long userId) {
//        return notificationMapper.findByUserId(userId);
//    }
//
//    public void markAsRead(Long notificationId) {
//        notificationMapper.markAsRead(notificationId, LocalDateTime.now());
//    }
//}
