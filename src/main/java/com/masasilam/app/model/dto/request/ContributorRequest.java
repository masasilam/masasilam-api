package com.masasilam.app.model.dto.request;

import lombok.Data;

@Data
public class ContributorRequest {
    private String name;

    private String role;
    private String websiteUrl;
}