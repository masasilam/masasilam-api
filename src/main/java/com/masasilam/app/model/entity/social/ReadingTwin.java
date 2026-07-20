package com.masasilam.app.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReadingTwin {
    private Long id;
    private Long userIdA;
    private Long userIdB;
    private Double similarityScore;
    private Integer commonCount;
    private LocalDateTime lastCalculated;
}