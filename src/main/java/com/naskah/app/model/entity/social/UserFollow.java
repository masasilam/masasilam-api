package com.naskah.app.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserFollow {
    private Long id;
    private Long followerId;
    private Long followingId;
    private LocalDateTime createdAt;
}