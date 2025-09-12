package com.naskah.demo.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReactionType {
    ANGRY("😠", "Angry"),
    CLAP("👏", "Clap"),
    CONFUSED("😕", "Confused"),
    EYES("👀", "Eyes"),
    HEARTBREAK("💔", "Heartbreak"),
    INFO("ℹ️", "Info"),
    LAUGH("😂", "Laugh"),
    LIKE("👍", "Like"),
    LOVE("❤️", "Love"),
    PARTY("🎉", "Party"),
    QUESTION("❓", "Question"),
    SAD("😢", "Sad"),
    STAR("⭐", "Star"),
    THUMBS_DOWN("👎", "Thumbs Down"),
    THUMBS_UP("👍", "Thumbs Up"),
    WOW("😲", "Wow");

    private final String emoji;
    private final String displayName;
}