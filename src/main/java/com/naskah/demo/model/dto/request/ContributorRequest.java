package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContributorRequest {
    private String name;

    private String role;
    private String websiteUrl;
}