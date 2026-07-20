package com.masasilam.app.model.dto.response.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupJoinRequestResponse {
    private Long id;
    private Long groupId;
    private Long userId;
    private String username;
    private String userPhoto;
    private String message;
    private String status;
    private LocalDateTime createdAt;
}