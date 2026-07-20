package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AnnotationsPageResponse {
    private List<AnnotationItemResponse> items;
    private Integer                      total;
    private Integer                      page;
    private Integer                      limit;
}