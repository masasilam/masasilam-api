package com.masasilam.app.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GuestReadLog {
    private Long id;
    private String guestId;
    private Long bookId;
    private Long zineId;
    private LocalDateTime createdAt;
}