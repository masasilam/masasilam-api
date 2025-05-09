package com.naskah.demo.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ErrorsResponse {
    String result ;
    String detail ;
    String path ;
    String date ;
    int code ;
    String version ;
    List<String> errors;
}
