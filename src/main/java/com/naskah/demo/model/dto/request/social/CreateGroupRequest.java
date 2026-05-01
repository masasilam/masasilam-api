package com.naskah.demo.model.dto.request.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CreateGroupRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 3000)
    private String description;

    private String groupType;  // public, private, invite_only
    private String focusType;  // BOOK, ZINE, FILM, NEWSPAPER, mixed
    private Integer maxMembers;
    private List<String> tags;
    private String rules;
}