package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserActivityResponse {
    private Long id;
    private String activityType;
    private String description;
    private Long projectId;
    private String projectTitle;
    private Integer pageNumber;
    private LocalDateTime createdAt;
}