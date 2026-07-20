package com.masasilam.app.model.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReadingGoal {
    private Long          id;
    private Long          userId;
    private String        title;
    private String        description;
    private String        goalType;      // books | minutes | streak
    private Integer       targetValue;
    private Integer       currentValue;
    private String        unit;          // buku | menit | hari
    private LocalDate     startDate;
    private LocalDate     endDate;
    private String        status;        // ACTIVE | COMPLETED | FAILED
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}