package com.naskah.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

@RestController
@RequestMapping("/api/translate")
@CrossOrigin(origins = "*")
public class TranslateController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Translate subtitle batch
     * POST /api/translate/batch
     * Body: { "texts": ["line1", "line2", ...], "targetLang": "id" }
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> translateBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) request.get("texts");
            String targetLang = (String) request.getOrDefault("targetLang", "id");

            if (texts == null || texts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No texts provided"));
            }

            System.out.println("Translating " + texts.size() + " lines to " + targetLang);

            // Batch translate using Google Translate
            List<String> translatedTexts = new ArrayList<>();
            int batchSize = 50; // Process 50 lines at a time

            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);

                // Join batch with a UNIQUE delimiter that won't appear in subtitles
                String combinedText = String.join(" |||DELIM||| ", batch);

                // Call Google Translate API
                String translated = callGoogleTranslate(combinedText, "en", targetLang);

                // PENTING: Decode URL-encoded characters yang mungkin ada
                try {
                    translated = URLDecoder.decode(translated, "UTF-8");
                } catch (Exception e) {
                    // Already decoded or not URL-encoded, continue
                }

                // Split back into individual lines - handle variations in delimiter
                // Google Translate might add/remove spaces around delimiter
                String[] translatedBatch;

                if (translated.contains(" |||DELIM||| ")) {
                    translatedBatch = translated.split(" \\|\\|\\|DELIM\\|\\|\\| ");
                } else if (translated.contains("|||DELIM|||")) {
                    translatedBatch = translated.split("\\|\\|\\|DELIM\\|\\|\\|");
                } else {
                    // Fallback: delimiter might be corrupted
                    System.err.println("Warning: Delimiter not found in translation, using original batch");
                    translatedBatch = batch.toArray(new String[0]);
                }

                // Clean up each translated line
                for (String line : translatedBatch) {
                    translatedTexts.add(line.trim());
                }

                // Progress log
                int progress = (int) (((double) end / texts.size()) * 100);
                System.out.println("Translation progress: " + progress + "%");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("translatedTexts", translatedTexts);
            response.put("originalCount", texts.size());
            response.put("translatedCount", translatedTexts.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Translation error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Translation failed: " + e.getMessage()));
        }
    }

    /**
     * Call Google Translate API (unofficial free endpoint)
     */
    private String callGoogleTranslate(String text, String sourceLang, String targetLang) {
        try {
            // Using unofficial Google Translate API (free, no rate limit)
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl="
                    + sourceLang + "&tl=" + targetLang + "&dt=t&q="
                    + URLEncoder.encode(text, "UTF-8");

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getBody() != null) {
                // Parse Google Translate response
                // Format: [[["translated text","original text",null,null,10]],null,"en",null,null,null,null,[]]
                String jsonResponse = response.getBody();

                // Extract translated text from JSON array
                JsonNode root = mapper.readTree(jsonResponse);
                if (root.isArray() && root.size() > 0) {
                    JsonNode firstArray = root.get(0);
                    if (firstArray.isArray() && firstArray.size() > 0) {
                        StringBuilder translatedText = new StringBuilder();
                        for (JsonNode translation : firstArray) {
                            if (translation.isArray() && translation.size() > 0) {
                                String translatedPart = translation.get(0).asText();
                                translatedText.append(translatedPart);
                            }
                        }
                        return translatedText.toString();
                    }
                }
            }

            // Fallback to original text
            return text;

        } catch (Exception e) {
            System.err.println("Google Translate API error: " + e.getMessage());
            // Fallback to original text
            return text;
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "translation",
                "provider", "Google Translate"
        ));
    }
}