package com.naskah.demo.model.film;

import lombok.Data;

@Data
public class Person {
    private Long id;
    private String wikidataQid;
    private String name;
    private String slug;
    private String photoUrl;
    private String description;
}