package com.masasilam.app.service.common.impl;

import com.masasilam.app.exception.custom.UnauthorizedException;
import com.masasilam.app.mapper.reading.ReadingGoalMapper;
import com.masasilam.app.mapper.reading.ReadingProgressMapper;
import com.masasilam.app.mapper.reading.ReadingSessionMapper;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.model.dto.request.GoalRequest;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.entity.ReadingGoal;
import com.masasilam.app.model.entity.ReadingProgress;
import com.masasilam.app.model.entity.ReadingSession;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.service.common.GoalService;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {
    private final ReadingGoalMapper goalMapper;
    private final ReadingSessionMapper sessionMapper;
    private final ReadingProgressMapper progressMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";
    private static final String ACTIVE = "ACTIVE";
    private static final String BOOKS = "books";
    private static final String MINUTES = "minutes";
    private static final String STREAK = "streak";

    private User getCurrentUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.trim().isEmpty()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    @Override
    public DataResponse<GoalsResponse> getGoals() {
        User user = getCurrentUser();
        Long userId = user.getId();

        syncGoalProgress(userId);

        List<ReadingGoal> activeGoals = goalMapper.findActiveByUser(userId);
        List<ReadingGoal> completedGoals = goalMapper.findCompletedByUser(userId);

        GoalsSummary summary = new GoalsSummary();
        summary.setTotal(goalMapper.countByUser(userId));
        summary.setActive(goalMapper.countActiveByUser(userId));
        summary.setCompleted(goalMapper.countCompletedByUser(userId));
        summary.setThisMonth(goalMapper.countThisMonthByUser(userId));

        GoalsResponse response = new GoalsResponse();
        response.setSummary(summary);
        response.setActive(activeGoals.stream().map(this::toResponse).collect(Collectors.toList()));
        response.setCompleted(completedGoals.stream().map(this::toResponse).collect(Collectors.toList()));

        return new DataResponse<>(SUCCESS, "Goals retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    public DataResponse<Void> createGoal(GoalRequest request) {
        User user = getCurrentUser();

        ReadingGoal goal = new ReadingGoal();
        goal.setUserId(user.getId());
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setGoalType(request.getGoalType());
        goal.setTargetValue(request.getTargetValue());
        goal.setCurrentValue(0);
        goal.setUnit(request.getUnit());
        goal.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
        goal.setEndDate(request.getEndDate());
        goal.setStatus(ACTIVE);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        goalMapper.insert(goal);

        return new DataResponse<>(SUCCESS, "Goal created", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<Void> updateGoal(Long id, GoalRequest request) {
        User user = getCurrentUser();

        ReadingGoal goal = goalMapper.findById(id);
        if (goal == null || !goal.getUserId().equals(user.getId())) {
            return new DataResponse<>("Error", "Goal tidak ditemukan", HttpStatus.NOT_FOUND.value(), null);
        }

        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setGoalType(request.getGoalType());
        goal.setTargetValue(request.getTargetValue());
        goal.setUnit(request.getUnit());
        goal.setStartDate(request.getStartDate());
        goal.setEndDate(request.getEndDate());
        goal.setUpdatedAt(LocalDateTime.now());

        goalMapper.update(goal);

        return new DataResponse<>(SUCCESS, "Goal updated", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<Void> deleteGoal(Long id) {
        User user = getCurrentUser();
        goalMapper.delete(id, user.getId());
        return new DataResponse<>(SUCCESS, "Goal deleted", HttpStatus.OK.value(), null);
    }

    private void syncGoalProgress(Long userId) {
        List<ReadingGoal> activeGoals = goalMapper.findActiveByUser(userId);
        if (activeGoals.isEmpty()) return;

        for (ReadingGoal goal : activeGoals) {
            int newValue = calculateCurrentValue(userId, goal);
            goalMapper.updateCurrentValue(goal.getId(), newValue);

            if (newValue >= goal.getTargetValue()) {
                goalMapper.markCompleted(goal.getId());
            }
        }
    }

    private int calculateCurrentValue(Long userId, ReadingGoal goal) {
        LocalDateTime since = goal.getStartDate().atStartOfDay();
        LocalDateTime until = goal.getEndDate().plusDays(1).atStartOfDay();

        List<ReadingSession> sessions = sessionMapper.findUserSessionsBetween(userId, since, until);

        return switch (goal.getGoalType()) {
            case BOOKS -> (int) sessions.stream()
                    .map(ReadingSession::getBookId)
                    .distinct()
                    .filter(bookId -> {
                        ReadingProgress p = progressMapper.findByUserAndBook(userId, bookId);
                        return p != null && p.getPercentageCompleted() != null && p.getPercentageCompleted().doubleValue() >= 95.0;
                    })
                    .count();

            case MINUTES -> sessions.stream()
                    .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0)
                    .sum();

            case STREAK -> calculateStreakDays(sessions);

            default -> 0;
        };
    }

    private int calculateStreakDays(List<ReadingSession> sessions) {
        if (sessions.isEmpty()) return 0;

        List<LocalDate> distinctDays = sessions.stream()
                .filter(s -> s.getStartedAt() != null)
                .map(s -> s.getStartedAt().toLocalDate())
                .distinct()
                .sorted((a, b) -> b.compareTo(a))
                .collect(Collectors.toList());

        int streak = 0;
        LocalDate expected = LocalDate.now();

        for (LocalDate day : distinctDays) {
            if (day.equals(expected) || day.equals(expected.minusDays(1))) {
                streak++;
                expected = day.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    private GoalItemResponse toResponse(ReadingGoal goal) {
        GoalItemResponse r = new GoalItemResponse();
        r.setId(goal.getId());
        r.setTitle(goal.getTitle());
        r.setDescription(goal.getDescription());
        r.setType(goal.getGoalType());
        r.setTarget(goal.getTargetValue());
        r.setCurrent(goal.getCurrentValue() != null ? goal.getCurrentValue() : 0);
        r.setUnit(goal.getUnit());
        r.setStartDate(goal.getStartDate());
        r.setEndDate(goal.getEndDate());
        r.setCompletedAt(goal.getCompletedAt());
        r.setStatus(goal.getStatus());

        int daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), goal.getEndDate());
        r.setDaysRemaining(Math.max(0, daysRemaining));

        return r;
    }
}