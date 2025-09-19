package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CollaborativeNoteRequest {
    @NotNull
    private Integer page;

    @NotNull
    private String position;

    @NotBlank
    private String content;

    private String title;
    private List<String> collaborators; // User IDs or emails
    private String visibility = "PRIVATE"; // PRIVATE, COLLABORATORS_ONLY, PUBLIC
}