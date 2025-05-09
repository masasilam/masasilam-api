package com.naskah.demo.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DatatableResponse<T> {
    String result ;
    String detail ;
    int code ;
    PageDataResponse<T> data;
}
