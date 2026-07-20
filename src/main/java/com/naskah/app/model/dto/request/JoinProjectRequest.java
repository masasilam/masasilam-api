package com.naskah.app.model.dto.request;

import com.naskah.app.model.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class JoinProjectRequest {

    @NotNull(message = "Role is required")
    private ProjectRole role;

    private List<Integer> pageNumbers;
}
