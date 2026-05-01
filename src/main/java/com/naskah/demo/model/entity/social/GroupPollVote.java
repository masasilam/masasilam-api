package com.naskah.demo.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupPollVote {
    private Long id;
    private Long pollId;
    private Long userId;
    private Integer optionId;
    private LocalDateTime createdAt;
}