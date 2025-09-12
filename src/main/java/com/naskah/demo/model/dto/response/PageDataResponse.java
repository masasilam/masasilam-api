package com.naskah.demo.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageDataResponse<T> {
    int page;
    int limit;
    Integer total;
    List<T> list;
}
