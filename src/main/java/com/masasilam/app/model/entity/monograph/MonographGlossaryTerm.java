package com.masasilam.app.model.entity.monograph;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonographGlossaryTerm {
    private Long id;
    private Long monographId;
    private String term;
    private String definition;
    private Integer termOrder;
}