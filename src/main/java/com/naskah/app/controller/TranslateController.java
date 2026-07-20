package com.naskah.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/translate")
@CrossOrigin(origins = "*")
public class TranslateController {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> translateBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) request.get("texts");
            String targetLang = (String) request.getOrDefault("targetLang", "id");

            if (texts == null || texts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No texts provided"));
            }

            List<String> translatedTexts = new ArrayList<>();
            int batchSize = 50;

            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);

                String combinedText = String.join(" |||DELIM||| ", batch);
                String translated = callGoogleTranslate(combinedText, targetLang);
                translated = URLDecoder.decode(translated, StandardCharsets.UTF_8);

                String[] translatedBatch;

                if (translated.contains(" |||DELIM||| ")) {
                    translatedBatch = translated.split(" \\|\\|\\|DELIM\\|\\|\\| ");
                } else if (translated.contains("|||DELIM|||")) {
                    translatedBatch = translated.split("\\|\\|\\|DELIM\\|\\|\\|");
                } else {
                    translatedBatch = batch.toArray(new String[0]);
                }

                for (String line : translatedBatch) {
                    translatedTexts.add(line.trim());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("translatedTexts", translatedTexts);
            response.put("originalCount", texts.size());
            response.put("translatedCount", translatedTexts.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Translation failed: " + e.getMessage()));
        }
    }

    private String callGoogleTranslate(String text, String targetLang) {
        try {
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=" + "en" + "&tl=" + targetLang + "&dt=t&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getBody() != null) {
                String jsonResponse = response.getBody();
                JsonNode root = mapper.readTree(jsonResponse);
                if (root.isArray() && !root.isEmpty()) {
                    JsonNode firstArray = root.get(0);
                    if (firstArray.isArray() && !firstArray.isEmpty()) {
                        StringBuilder translatedText = new StringBuilder();
                        for (JsonNode translation : firstArray) {
                            if (translation.isArray() && !translation.isEmpty()) {
                                String translatedPart = translation.get(0).asText();
                                translatedText.append(translatedPart);
                            }
                        }
                        return translatedText.toString();
                    }
                }
            }
            return text;
        } catch (Exception e) {
            return text;
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "translation",
                "provider", "Google Translate"
        ));
    }
}