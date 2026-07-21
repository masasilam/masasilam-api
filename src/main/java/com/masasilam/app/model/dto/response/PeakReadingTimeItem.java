package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class PeakReadingTimeItem {
    private Integer hour;
    private Integer minutesRead;
    private Double percentage;
}