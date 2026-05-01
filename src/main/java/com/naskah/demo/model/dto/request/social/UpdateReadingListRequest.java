package com.naskah.demo.model.dto.request.social;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UpdateReadingListRequest {
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    private String visibility;
    private List<String> tags;
}