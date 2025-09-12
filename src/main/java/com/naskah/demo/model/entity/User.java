package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private String profilePictureUrl;
    private String bio;
    private Integer totalBooksRead;
    private Integer readingStreakDays;
    private Boolean isActive;
    private Boolean emailVerified;
    private String status;
    private String googleId;
    private Integer contributedBooksCount;
    private Double averageRating;
    private Integer experiencePoints;
    private Boolean emailNotifications;
    private String level;
    private LocalDateTime lastActiveDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}