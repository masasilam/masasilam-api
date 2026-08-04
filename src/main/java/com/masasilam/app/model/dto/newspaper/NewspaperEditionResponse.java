package com.masasilam.app.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperEditionResponse {
    private LocalDate publishDate;
    private String dateFormatted;
    private Integer articleCount;
}