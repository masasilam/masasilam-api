package com.naskah.demo.model.dto.opds;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpdsAuthor {
    private String name;
    private String uri;
}