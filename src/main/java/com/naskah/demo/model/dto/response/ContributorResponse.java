package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContributorResponse {
    private Long id;
    private String name;
    private String slug;
    private String role;
    private String websiteUrl;
}
