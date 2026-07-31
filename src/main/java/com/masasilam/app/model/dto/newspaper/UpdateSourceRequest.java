package com.masasilam.app.model.dto.newspaper;

import lombok.Data;

@Data
public class UpdateSourceRequest {
    private String name;
    private String description;
    private String location;
    private String logoUrl;
}