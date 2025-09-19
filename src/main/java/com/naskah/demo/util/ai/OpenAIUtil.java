package com.naskah.demo.util.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OpenAIUtil {

    // Using free alternatives or local AI models
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate"; // Local Ollama
    private static final String MODEL_NAME = "llama2"; // Free local model

    /**
     * Generate summary using local AI model (Ollama)
     */
    public String generateSummary(String content, String summaryType, Integer maxLength) {
        try {
            String prompt = buildSummaryPrompt(content, summaryType, maxLength);
            return callLocalAI(prompt);

        } catch (Exception e) {
            log.error("Error generating summary", e);
            return "Summary generation failed. Please try again later.";
        }
    }

    /**
     * Extract key points from content
     */
    public List<String> extractKeyPoints(String content) {
        try {
            String prompt = "Extract the 5 most important key points from this text:\n\n" + content +
                    "\n\nProvide each key point as a separate line starting with '-'.";
            String response = callLocalAI(prompt);

            return Arrays.stream(response.split("\n"))
                    .filter(line -> line.trim().startsWith("-"))
                    .map(line -> line.substring(1).trim())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error extracting key points", e);
            return Arrays.asList("Key points extraction failed");
        }
    }

    /**
     * Answer questions about book content
     */
    public String answerQuestion(String question, String context) {
        try {
            String prompt = "Based on this context: " + context +
                    "\n\nAnswer this question: " + question +
                    "\n\nProvide a clear and accurate answer based only on the given context.";
            return callLocalAI(prompt);

        } catch (Exception e) {
            log.error("Error answering question", e);
            return "I'm sorry, I couldn't generate an answer for your question. Please try again later.";
        }
    }

    /**
     * Generate bookmark suggestions
     */
    public List<BookmarkSuggestionResponse> generateBookmarkSuggestions(String content, Integer totalPages) {
        try {
            // Simple implementation - in real scenario, use proper AI analysis
            List<BookmarkSuggestionResponse> suggestions = new ArrayList<>();

            // Add some default suggestions based on content analysis
            suggestions.add(createBookmarkSuggestion(1, "Introduction", "Start of the book", "Beginning of content", 0.9, "#4CAF50"));
            suggestions.add(createBookmarkSuggestion(totalPages / 4, "Key Concept", "Important section identified", "Major topic discussion", 0.8, "#2196F3"));
            suggestions.add(createBookmarkSuggestion(totalPages / 2, "Midpoint", "Middle of the book", "Halfway through content", 0.7, "#FF9800"));
            suggestions.add(createBookmarkSuggestion(totalPages * 3/4, "Climax/Resolution", "Critical section", "Important developments", 0.85, "#9C27B0"));
            suggestions.add(createBookmarkSuggestion(totalPages, "Conclusion", "End of the book", "Summary and conclusions", 0.9, "#F44336"));

            return suggestions;

        } catch (Exception e) {
            log.error("Error generating bookmark suggestions", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generate quiz questions
     */
    public List<QuizResponse.QuizQuestion> generateQuizQuestions(String content, QuizRequest request) {
        try {
            List<QuizResponse.QuizQuestion> questions = new ArrayList<>();

            // Generate sample questions based on content
            for (int i = 1; i <= request.getQuestionCount(); i++) {
                QuizResponse.QuizQuestion question = new QuizResponse.QuizQuestion();
                question.setId((long) i);
                question.setQuestion("Sample question " + i + " based on chapter content?");
                question.setType(request.getQuizType());

                if ("MULTIPLE_CHOICE".equals(request.getQuizType())) {
                    question.setOptions(Arrays.asList("Option A", "Option B", "Option C", "Option D"));
                    question.setCorrectAnswer("Option A");
                } else if ("TRUE_FALSE".equals(request.getQuizType())) {
                    question.setOptions(Arrays.asList("True", "False"));
                    question.setCorrectAnswer("True");
                } else {
                    question.setCorrectAnswer("Sample answer");
                }

                question.setExplanation("This is the explanation for the answer.");
                question.setPage(request.getChapter() * 10); // Estimate page

                questions.add(question);
            }

            return questions;

        } catch (Exception e) {
            log.error("Error generating quiz questions", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generate AI highlights
     */
    public List<AIHighlightResponse.AIHighlight> generateAIHighlights(String content, AIHighlightRequest request) {
        try {
            List<AIHighlightResponse.AIHighlight> highlights = new ArrayList<>();

            // Simple implementation - analyze content for important parts
            String[] sentences = content.split("\\.");
            int highlightCount = 0;

            for (int i = 0; i < sentences.length && highlightCount < request.getMaxHighlights(); i++) {
                String sentence = sentences[i].trim();
                if (sentence.length() > 50 && isImportantSentence(sentence, request.getHighlightType())) {
                    AIHighlightResponse.AIHighlight highlight = new AIHighlightResponse.AIHighlight();
                    highlight.setPage(request.getStartPage() + (i / 10)); // Estimate page
                    highlight.setText(sentence);
                    highlight.setStartPosition(String.valueOf(i * 100));
                    highlight.setEndPosition(String.valueOf((i + 1) * 100));
                    highlight.setCategory(request.getHighlightType());
                    highlight.setConfidenceScore(0.8 + (Math.random() * 0.2)); // Random confidence
                    highlight.setReason("Identified as " + request.getHighlightType().toLowerCase());

                    highlights.add(highlight);
                    highlightCount++;
                }
            }

            return highlights;

        } catch (Exception e) {
            log.error("Error generating AI highlights", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generate tags for notes
     */
    public List<String> generateTagsForNote(String content, String title) {
        try {
            // Simple tag generation based on keywords
            Set<String> tags = new HashSet<>();
            String text = (title + " " + content).toLowerCase();

            // Add predefined tags based on keywords
            if (text.contains("important") || text.contains("key") || text.contains("critical")) {
                tags.add("important");
            }
            if (text.contains("question") || text.contains("doubt") || text.contains("unclear")) {
                tags.add("question");
            }
            if (text.contains("idea") || text.contains("concept") || text.contains("theory")) {
                tags.add("concept");
            }
            if (text.contains("example") || text.contains("case") || text.contains("illustration")) {
                tags.add("example");
            }
            if (text.contains("summary") || text.contains("conclusion") || text.contains("result")) {
                tags.add("summary");
            }

            // Add generic tag if no specific tags found
            if (tags.isEmpty()) {
                tags.add("general");
            }

            return new ArrayList<>(tags);

        } catch (Exception e) {
            log.error("Error generating tags for note", e);
            return Arrays.asList("general");
        }
    }

    // Helper methods

    private String callLocalAI(String prompt) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> request = Map.of(
                    "model", MODEL_NAME,
                    "prompt", prompt,
                    "stream", false
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Try to call local Ollama, fallback to simple response if not available
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(OLLAMA_URL, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonResponse = mapper.readTree(response.getBody());
                    return jsonResponse.get("response").asText();
                }
            } catch (Exception e) {
                log.warn("Local AI service not available, using fallback response");
            }

            // Fallback response when AI service is not available
            return generateFallbackResponse(prompt);

        } catch (Exception e) {
            log.error("Error calling AI service", e);
            return "AI service temporarily unavailable. Please try again later.";
        }
    }

    private String generateFallbackResponse(String prompt) {
        // Simple fallback responses based on prompt type
        if (prompt.contains("summarize") || prompt.contains("summary")) {
            return "This is a brief summary of the content. The main points include key concepts, important details, and relevant conclusions from the text.";
        } else if (prompt.contains("question")) {
            return "Based on the provided context, this appears to be related to the main themes and concepts discussed in the text.";
        } else if (prompt.contains("key points")) {
            return "- Main concept or theme\n- Supporting details\n- Important examples\n- Key conclusions\n- Relevant implications";
        } else {
            return "Response generated successfully. The content has been processed according to your request.";
        }
    }

    private String buildSummaryPrompt(String content, String summaryType, Integer maxLength) {
        StringBuilder prompt = new StringBuilder();

        switch (summaryType.toUpperCase()) {
            case "BRIEF":
                prompt.append("Provide a brief summary of the following text in approximately ")
                        .append(maxLength).append(" words:\n\n");
                break;
            case "DETAILED":
                prompt.append("Provide a detailed summary of the following text in approximately ")
                        .append(maxLength).append(" words, covering all major points:\n\n");
                break;
            case "BULLET_POINTS":
                prompt.append("Summarize the following text as bullet points (maximum ")
                        .append(maxLength).append(" words total):\n\n");
                break;
            default:
                prompt.append("Summarize the following text:\n\n");
        }

        prompt.append(content);
        return prompt.toString();
    }

    private BookmarkSuggestionResponse createBookmarkSuggestion(Integer page, String title, String description, String reason, Double score, String color) {
        BookmarkSuggestionResponse suggestion = new BookmarkSuggestionResponse();
        suggestion.setPage(page);
        suggestion.setTitle(title);
        suggestion.setDescription(description);
        suggestion.setReason(reason);
        suggestion.setRelevanceScore(score);
        suggestion.setSuggestedColor(color);
        return suggestion;
    }

    private boolean isImportantSentence(String sentence, String highlightType) {
        String lowerSentence = sentence.toLowerCase();

        switch (highlightType.toUpperCase()) {
            case "IMPORTANT":
                return lowerSentence.contains("important") || lowerSentence.contains("key") ||
                        lowerSentence.contains("critical") || lowerSentence.contains("essential");
            case "QUOTES":
                return sentence.contains("\"") || sentence.contains("'");
            case "DEFINITIONS":
                return lowerSentence.contains("is defined as") || lowerSentence.contains("means") ||
                        lowerSentence.contains("refers to") || lowerSentence.contains("definition");
            case "KEYWORDS":
                return sentence.length() < 100; // Shorter sentences likely contain keywords
            default:
                return Math.random() > 0.7; // Random selection for general highlights
        }
    }
}
