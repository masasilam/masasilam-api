package com.masasilam.app.model.entity.monograph;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonographCriticalNote {
    private Long id;
    private Long monographId;
    private Integer noteNumber;
    private String noteText;
    private Long relatedArticleId;
}