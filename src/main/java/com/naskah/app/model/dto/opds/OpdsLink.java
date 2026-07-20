package com.naskah.app.model.dto.opds;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpdsLink {
    private String rel;
    private String href;
    private String type;
    private String title;

    public OpdsLink(String rel, String href, String type) {
        this.rel = rel;
        this.href = href;
        this.type = type;
    }
}