package com.masasilam.app.model.dto.response.social;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TimeCapsuleResponse {
    private LocalDate historicalDate;
    private String formattedDate;
    private Integer yearDifference;
    private List<TimeCapsuleContentResponse> articles;
    private List<String> participatingUsers; // usernames reading same date today
    private Integer totalReaders;
}