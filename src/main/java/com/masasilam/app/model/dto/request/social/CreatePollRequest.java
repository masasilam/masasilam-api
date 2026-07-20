package com.masasilam.app.model.dto.request.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreatePollRequest {
    @NotBlank
    private String question;

    @NotEmpty
    private List<String> options;

    private LocalDateTime endsAt;
}