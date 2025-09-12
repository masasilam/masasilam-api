package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegisterResponse {
    private String username;
    private String email;
    private String fullName;
    private String message;
    private boolean emailVerified;
    private LocalDateTime registeredAt;
}
