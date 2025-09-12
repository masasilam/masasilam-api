package com.naskah.demo.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BlogPostStatus {
    DRAFT("Draft"),
    PUBLISHED("Published"),
    SCHEDULED("Scheduled"),
    ARCHIVED("Archived");

    private final String displayName;
}
