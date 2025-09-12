//package com.naskah.demo.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class ProjectPageService {
//
//    private final ProjectPageMapper projectPageMapper;
//    private final UserService userService;
//    private final NotificationService notificationService;
//
//    public Optional<ProjectPage> findById(Long id) {
//        return projectPageMapper.findById(id);
//    }
//
//    public List<ProjectPage> findByProject(Long projectId) {
//        return projectPageMapper.findByProjectId(projectId);
//    }
//
//    public List<ProjectPage> findAvailableForUser(User user) {
//        PageStatus targetStatus = getTargetStatusForUser(user);
//        return projectPageMapper.findAvailableByStatus(targetStatus);
//    }
//
//    private PageStatus getTargetStatusForUser(User user) {
//        return switch (user.getLevel()) {
//            case P1 -> PageStatus.P1_AVAILABLE;
//            case P2 -> PageStatus.P2_AVAILABLE;
//            case P3 -> PageStatus.P3_AVAILABLE;
//        };
//    }
//
//    public ProjectPage checkoutPage(Long pageId, Long userId) {
//        ProjectPage page = findById(pageId).orElseThrow(() -> new RuntimeException("Page not found"));
//        User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//        // Validate page availability
//        PageStatus expectedStatus = getTargetStatusForUser(user);
//        if (page.getStatus() != expectedStatus) {
//            throw new RuntimeException("Page not available for checkout");
//        }
//
//        // Check if user already has pages checked out
//        List<ProjectPage> checkedOutPages = projectPageMapper.findCheckedOutByUser(userId);
//        if (checkedOutPages.size() >= getMaxCheckoutLimit(user)) {
//            throw new RuntimeException("Maximum checkout limit reached");
//        }
//
//        // Checkout page
//        page.setCheckedOutBy(userId);
//        page.setCheckedOutAt(LocalDateTime.now());
//        page.setStatus(getCheckedOutStatus(user.getLevel()));
//
//        projectPageMapper.update(page);
//
//        notificationService.sendPageCheckedOutNotification(user, page);
//
//        return page;
//    }
//
//    private int getMaxCheckoutLimit(User user) {
//        return switch (user.getLevel()) {
//            case P1 -> 2;
//            case P2 -> 3;
//            case P3 -> 5;
//        };
//    }
//
//    private PageStatus getCheckedOutStatus(UserLevel level) {
//        return switch (level) {
//            case P1 -> PageStatus.P1_CHECKED_OUT;
//            case P2 -> PageStatus.P2_CHECKED_OUT;
//            case P3 -> PageStatus.P3_CHECKED_OUT;
//        };
//    }
//
//    public ProjectPage submitPage(Long pageId, String text, String notes) {
//        ProjectPage page = findById(pageId).orElseThrow();
//        User user = userService.findById(page.getCheckedOutBy()).orElseThrow();
//
//        // Update page content based on level
//        switch (user.getLevel()) {
//            case P1 -> page.setP1Text(text);
//            case P2 -> page.setP2Text(text);
//            case P3 -> page.setP3Text(text);
//        }
//
//        page.setNotes(notes);
//        page.setCompletedAt(LocalDateTime.now());
//        page.setStatus(getCompletedStatus(user.getLevel()));
//        page.setRevisionCount(page.getRevisionCount() + 1);
//
//        projectPageMapper.update(page);
//
//        // Update user progress
//        userService.updateUserProgress(user.getId(), user.getPagesCompleted() + 1);
//
//        notificationService.sendPageCompletedNotification(user, page);
//
//        return page;
//    }
//
//    private PageStatus getCompletedStatus(UserLevel level) {
//        return switch (level) {
//            case P1 -> PageStatus.P1_COMPLETED;
//            case P2 -> PageStatus.P2_COMPLETED;
//            case P3 -> PageStatus.P3_COMPLETED;
//        };
//    }
//
//    public void releasePage(Long pageId) {
//        ProjectPage page = findById(pageId).orElseThrow();
//        page.setCheckedOutBy(null);
//        page.setCheckedOutAt(null);
//        page.setStatus(getAvailableStatusFromCheckedOut(page.getStatus()));
//
//        projectPageMapper.update(page);
//    }
//
//    private PageStatus getAvailableStatusFromCheckedOut(PageStatus checkedOutStatus) {
//        return switch (checkedOutStatus) {
//            case P1_CHECKED_OUT -> PageStatus.P1_AVAILABLE;
//            case P2_CHECKED_OUT -> PageStatus.P2_AVAILABLE;
//            case P3_CHECKED_OUT -> PageStatus.P3_AVAILABLE;
//            default -> checkedOutStatus;
//        };
//    }
//
//    public List<ProjectPage> findCheckedOutByUser(Long userId) {
//        return projectPageMapper.findCheckedOutByUser(userId);
//    }
//}