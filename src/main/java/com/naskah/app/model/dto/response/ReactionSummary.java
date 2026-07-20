package com.naskah.app.model.dto.response;

import com.naskah.app.model.enums.ReactionType;
import lombok.Data;

@Data
public class ReactionSummary {
    private ReactionType reactionType;
    private String displayName;
    private Long count;
}