package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String tokenType;
    private String refreshToken;
    private String username;
    private String name;
    private String[] roles;
    private Long expiresIn;
}
