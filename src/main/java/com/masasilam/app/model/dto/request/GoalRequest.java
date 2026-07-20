package com.masasilam.app.model.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class GoalRequest {
    private String    title;
    private String    description;
    private String    goalType;    // books | minutes | streak
    private Integer   targetValue;
    private String    unit;        // buku | menit | hari
    private LocalDate startDate;
    private LocalDate endDate;
}
