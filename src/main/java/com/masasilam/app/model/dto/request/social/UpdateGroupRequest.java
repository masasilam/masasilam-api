package com.masasilam.app.model.dto.request.social;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UpdateGroupRequest {
    @Size(max = 255)
    private String name;

    @Size(max = 3000)
    private String description;

    private String groupType;
    private Integer maxMembers;
    private List<String> tags;
    private String rules;
}