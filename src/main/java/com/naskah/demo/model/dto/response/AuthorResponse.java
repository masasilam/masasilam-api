package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorResponse {
    private Long id;
    private String name;
    private String slug;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private String birthPlace;
    private String nationality;
    private String photoUrl;
}
