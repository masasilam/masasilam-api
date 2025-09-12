package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Language {
    private Integer id;
    private String code;
    private String name;
    private String nativeName;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}
