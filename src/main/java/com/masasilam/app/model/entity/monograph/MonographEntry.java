package com.masasilam.app.model.entity.monograph;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonographEntry {
    private Long id;
    private Long monographId;
    private Long chapterId;
    private Long articleId;
    private String role;
    private String textStatus;
    private Integer entryOrder;
    private String positionLabel;
    private String columnInfo;
    private String editorialNote;
}