package com.masasilam.app.model.dto.request.social;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VotePollRequest {
    @NotNull
    private Integer optionId;
}