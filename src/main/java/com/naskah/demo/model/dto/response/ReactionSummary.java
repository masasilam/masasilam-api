package com.naskah.demo.model.dto.response;

import com.naskah.demo.model.enums.ReactionType;
import lombok.Data;

@Data
public class ReactionSummary {
    private ReactionType reactionType;
    private String displayName;
    private Long count;
}