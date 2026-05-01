package com.naskah.demo.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupPoll {
    private Long id;
    private Long groupId;
    private Long createdBy;
    private String question;
    private String options;  // JSON: [{id, text, vote_count}]
    private LocalDateTime endsAt;
    private Boolean isClosed;
    private LocalDateTime createdAt;
}