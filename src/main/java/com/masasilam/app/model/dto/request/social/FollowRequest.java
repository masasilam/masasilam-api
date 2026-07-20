package com.masasilam.app.model.dto.request.social;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FollowRequest {
    @NotNull
    private Long userId;
}