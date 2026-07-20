package com.naskah.app.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class CalendarResponse {
    private List<CalendarDayResponse> days;
    private Integer                   totalMinutes;
    private Integer                   totalPages;
    private Integer                   activeDays;
}