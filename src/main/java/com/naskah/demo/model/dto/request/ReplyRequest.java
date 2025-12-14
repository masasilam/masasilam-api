package com.naskah.demo.model.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReplyRequest {
    @NotBlank(message = "Reply content is required")
    @Size(min = 1, max = 2000, message = "Reply must be between 1 and 2000 characters")
    private String content;
}

