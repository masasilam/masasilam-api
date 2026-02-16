package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    private String profilePictureUrl;

    private Boolean emailNotifications;
}