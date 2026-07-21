package com.masasilam.app.model.entity.social;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupPoll {
    private Long id;
    private Long groupId;
    private Long createdBy;
    private String question;
    private String options;
    private LocalDateTime endsAt;
    private Boolean isClosed;
    private LocalDateTime createdAt;
}