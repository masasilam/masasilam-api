package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ExportHistoryResponse {
    private List<ExportAnnotationsResponse> exports;
    private Integer totalExports;
    private Long totalBytesExported;
}
