package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AudioSyncRequest {
    @NotNull
    private Integer page;

    @NotNull
    private String textPosition;

    @NotNull
    private Double audioTimestamp; // in seconds
}