package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class LibraryPageResponse {
    private List<LibraryBookResponse> items;
    private Integer                   totalData;
    private Integer                   page;
    private Integer                   limit;
}
