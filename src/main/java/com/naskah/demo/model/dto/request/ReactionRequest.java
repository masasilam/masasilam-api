package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ReactionRequest {
    private String type; // "rating", "angry", "like", "love", "comment"
    private Integer rating; // Untuk type "rating"
    private String comment; // Ini yang menggantikan diskusi
    private String title; // Optional title untuk comment yang panjang
    private Integer page; // Halaman di mana reaction diberikan
    private String position; // Posisi spesifik di halaman
    private Long parentId; // Untuk reply ke reaction lain (threading)
}