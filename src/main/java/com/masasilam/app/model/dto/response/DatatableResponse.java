package com.masasilam.app.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DatatableResponse<T> {
    String result;
    String detail;
    int code;
    PageDataResponse<T> data;
}
