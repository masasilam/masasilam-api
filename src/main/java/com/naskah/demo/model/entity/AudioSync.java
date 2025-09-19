package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AudioSync {
    private Long id;
    private Long bookId;
    private Integer page;
    private String textPosition;
    private Double audioTimestamp;
    private String text;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}