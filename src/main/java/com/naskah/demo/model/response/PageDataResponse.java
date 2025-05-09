package com.naskah.demo.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PageDataResponse<T> {
    int page;
    int limit;
    Integer total;
    List<T> list;
}
