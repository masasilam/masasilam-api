package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ReadingHistoryPageResponse {
    private List<ReadingHistoryItemResponse> list;
    private Integer total;
    private Integer page;
    private Integer limit;
}