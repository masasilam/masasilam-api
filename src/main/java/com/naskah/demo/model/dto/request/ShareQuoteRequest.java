package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShareQuoteRequest {
    @NotBlank
    private String text;

    private String authorName;
    private Integer page;
    private String template = "DEFAULT"; // Template style
    private String backgroundColor = "#FFFFFF";
    private String textColor = "#000000";
}