package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReadingGoalsResponse {
    private List<ReadingGoal> activeGoals;
    private List<ReadingGoal> completedGoals;
    private GoalsSummary summary;

    @Data
    public static class ReadingGoal {
        private Long goalId;
        private String goalType;            // "books_per_year", "minutes_per_day", etc.
        private String title;
        private String description;
        private Integer targetValue;
        private Integer currentValue;
        private Double progressPercentage;
        private String deadline;
        private String status;              // "active", "completed", "failed"
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    @Data
    public static class GoalsSummary {
        private Integer totalGoals;
        private Integer activeGoals;
        private Integer completedGoals;
        private Double overallProgress;
        private Boolean onTrack;
    }
}
