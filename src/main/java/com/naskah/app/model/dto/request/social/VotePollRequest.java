package com.naskah.app.model.dto.request.social;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VotePollRequest {
    @NotNull
    private Integer optionId;
}