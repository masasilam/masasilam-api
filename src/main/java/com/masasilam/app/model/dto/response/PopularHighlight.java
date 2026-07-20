package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class PopularHighlight {
    private String text;
    private Integer highlightCount;
    private Integer position;
}