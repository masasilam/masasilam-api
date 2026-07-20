package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private Long id;
    private String token;
    private String refreshToken;
    private String tokenType;
    private String username;
    private String name;
    private String email;
    private String[] roles;
    private Long expiresIn;

    // ✅ TAMBAHKAN FIELD INI
    private String fullName;
    private String profilePictureUrl;
    private String bio;
    private Boolean emailNotifications;
    private String level;
    private Integer totalBooksRead;
    private Integer readingStreakDays;
    private Integer contributedBooksCount;
    private Double averageRating;
    private Integer experiencePoints;
}