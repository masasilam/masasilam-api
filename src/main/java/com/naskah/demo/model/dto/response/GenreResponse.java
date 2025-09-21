package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class GenreResponse {
    private Long id;
    private String name;
    private String slug;
    private Boolean isFiction;
}
