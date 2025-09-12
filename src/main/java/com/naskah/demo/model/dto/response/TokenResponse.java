package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class TokenResponse {
    private String token;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}