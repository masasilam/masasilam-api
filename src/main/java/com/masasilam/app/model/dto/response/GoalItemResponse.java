package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GoalItemResponse {
    private Long          id;
    private String        title;
    private String        description;
    private String        type;          // books | minutes | streak
    private Integer       target;
    private Integer       current;
    private String        unit;
    private LocalDate     start_date;
    private LocalDate     end_date;
    private Integer       days_remaining;
    private LocalDateTime completed_at;
    private String        status;
}