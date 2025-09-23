package com.naskah.demo.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataResponse<T> {
    String result;
    String detail;
    int code;
    T data;
}
