package com.masasilam.app.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DefaultResponse {
    String result;
    String detail;
    int code;
}
