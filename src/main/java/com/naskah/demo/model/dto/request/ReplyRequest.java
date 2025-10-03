package com.naskah.demo.model.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplyRequest {
    @NotBlank(message = "Reply content is required")
    private String comment;
}

