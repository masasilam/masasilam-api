package com.naskah.demo.model.dto.request;

import lombok.Data;

@Data
public class ReactionRequest {
    private String type; // Frontend mengirim "type"
    private Integer rating; // Frontend mengirim rating untuk type "rating"
    private String comment; // Frontend mengirim comment
    private Integer page;
    private String position;
}