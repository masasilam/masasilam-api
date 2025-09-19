package com.naskah.demo.util.voice;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class VoiceCommandProcessor {

    /**
     * Convert speech to text (simplified implementation)
     */
    public String convertSpeechToText(MultipartFile audioFile) {
        try {
            // In real implementation, use speech-to-text service like Google Speech API
            // For demo purposes, return sample commands
            byte[] audioData = audioFile.getBytes();

            // Mock recognition based on file size (just for demo)
            if (audioData.length > 10000) {
                return "go to page fifty";
            } else if (audioData.length > 5000) {
                return "search for important concepts";
            } else {
                return "add bookmark here";
            }

        } catch (IOException e) {
            log.error("Error converting speech to text", e);
            return "unrecognized command";
        }
    }

    /**
     * Parse voice command and extract action
     */
    public VoiceCommand parseVoiceCommand(String recognizedText) {
        VoiceCommand command = new VoiceCommand();
        command.setOriginalText(recognizedText.toLowerCase());

        // Navigation commands
        if (parseNavigationCommand(recognizedText, command)) {
            return command;
        }

        // Search commands
        if (parseSearchCommand(recognizedText, command)) {
            return command;
        }

        // Bookmark commands
        if (parseBookmarkCommand(recognizedText, command)) {
            return command;
        }

        // Reading control commands
        if (parseReadingControlCommand(recognizedText, command)) {
            return command;
        }

        // Unknown command
        command.setAction("UNKNOWN");
        command.setValid(false);
        return command;
    }

    private boolean parseNavigationCommand(String text, VoiceCommand command) {
        // "go to page X" or "navigate to page X"
        Pattern pagePattern = Pattern.compile("(?:go to|navigate to)\\s+page\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher pageMatcher = pagePattern.matcher(text);

        if (pageMatcher.find()) {
            command.setAction("NAVIGATE_TO_PAGE");
            command.setTargetPage(Integer.parseInt(pageMatcher.group(1)));
            command.setValid(true);
            return true;
        }

        // "next page" or "previous page"
        if (text.contains("next page")) {
            command.setAction("NEXT_PAGE");
            command.setValid(true);
            return true;
        }

        if (text.contains("previous page") || text.contains("prev page")) {
            command.setAction("PREVIOUS_PAGE");
            command.setValid(true);
            return true;
        }

        return false;
    }

    private boolean parseSearchCommand(String text, VoiceCommand command) {
        // "search for X"
        Pattern searchPattern = Pattern.compile("search for\\s+(.+)", Pattern.CASE_INSENSITIVE);
        Matcher searchMatcher = searchPattern.matcher(text);

        if (searchMatcher.find()) {
            command.setAction("SEARCH");
            command.setSearchQuery(searchMatcher.group(1).trim());
            command.setValid(true);
            return true;
        }

        return false;
    }

    private boolean parseBookmarkCommand(String text, VoiceCommand command) {
        // "add bookmark" or "bookmark this page"
        if (text.contains("add bookmark") || text.contains("bookmark this")) {
            command.setAction("ADD_BOOKMARK");

            // Try to extract bookmark title
            Pattern titlePattern = Pattern.compile("(?:add bookmark|bookmark this)(?:\\s+(?:as|called|named)\\s+(.+))?", Pattern.CASE_INSENSITIVE);
            Matcher titleMatcher = titlePattern.matcher(text);

            if (titleMatcher.find() && titleMatcher.group(1) != null) {
                command.setBookmarkTitle(titleMatcher.group(1).trim());
            } else {
                command.setBookmarkTitle("Voice Bookmark");
            }

            command.setValid(true);
            return true;
        }

        return false;
    }

    private boolean parseReadingControlCommand(String text, VoiceCommand command) {
        // "start reading" or "read aloud"
        if (text.contains("start reading") || text.contains("read aloud")) {
            command.setAction("START_READING");
            command.setValid(true);
            return true;
        }

        // "stop reading"
        if (text.contains("stop reading")) {
            command.setAction("STOP_READING");
            command.setValid(true);
            return true;
        }

        return false;
    }

    @Data
    public static class VoiceCommand {
        private String originalText;
        private String action;
        private boolean valid = false;
        private Integer targetPage;
        private String searchQuery;
        private String bookmarkTitle;
    }
}
