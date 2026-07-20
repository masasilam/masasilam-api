package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class CalendarDayResponse {
    private Integer              day;
    private Integer              minutesRead;
    private Integer              pagesRead;
    private List<CalendarBookEntry> books;
}