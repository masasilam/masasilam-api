package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class MemberRoleSummary {
    private String role;
    private String displayName;
    private Integer count;
}