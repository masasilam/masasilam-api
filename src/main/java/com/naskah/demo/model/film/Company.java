package com.naskah.demo.model.film;

import lombok.Data;

@Data
public class Company {
    private Long id;
    private String wikidataQid;
    private String name;
    private String slug;
    private String logoUrl;
    private String description;
}
