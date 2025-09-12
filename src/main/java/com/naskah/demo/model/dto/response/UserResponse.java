package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String profilePictureUrl;
    private String bio;
    private Integer totalBooksRead;
    private Integer readingStreakDays;
    private Integer contributedBooksCount;
    private Double averageRating;
    private Integer experiencePoints;
    private String level;
}
