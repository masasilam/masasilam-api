package com.masasilam.app.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendData {
    private String direction;
    private Double changePercentage;
    private String interpretation;
}