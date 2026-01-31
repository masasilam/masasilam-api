
package com.naskah.demo.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// ============================================
// REQUEST DTOs
// ============================================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperSearchCriteria {
    private String searchQuery;
    private String category;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String source;
    private String importance;
    private List<String> tags;
}