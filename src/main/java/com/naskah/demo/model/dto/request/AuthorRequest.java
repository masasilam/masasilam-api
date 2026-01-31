package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AuthorRequest {
    @NotBlank
    private String name;

    private String birthDate;
    private String deathDate;
    private String birthPlace;
    private String nationality;
    private String biography;
    private MultipartFile photo;
}
