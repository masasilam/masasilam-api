package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ReactionRequest {
    private String type;
    private Integer rating;
    private String comment;
    private String title;
    private Long parentId;
}