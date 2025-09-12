package com.naskah.demo.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommentType {
    FEEDBACK("Feedback"),
    SUGGESTION("Suggestion"),
    QUESTION("Question"),
    APPRECIATION("Appreciation"),
    CORRECTION("Correction"),
    GENERAL("General Comment");

    private final String displayName;
}