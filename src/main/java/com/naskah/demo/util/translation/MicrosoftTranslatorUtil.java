package com.naskah.demo.util.translation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MicrosoftTranslatorUtil {

    // Using free Microsoft Translator service
    private static final String TRANSLATE_URL = "https://api.cognitive.microsofttranslator.com/translate";
    private static final String API_VERSION = "3.0";

    /**
     * Translate text using Microsoft Translator (free tier)
     */
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Build URL with parameters
            String url = TRANSLATE_URL + "?api-version=" + API_VERSION + "&to=" + targetLanguage;
            if (sourceLanguage != null && !sourceLanguage.isEmpty()) {
                url += "&from=" + sourceLanguage;
            }

            // Prepare request body
            Map<String, String> requestBody = Map.of("Text", text);
            List<Map<String, String>> requestList = Collections.singletonList(requestBody);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<List<Map<String, String>>> entity = new HttpEntity<>(requestList, headers);

            // Make request
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.POST, entity, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> responseList = response.getBody();
                if (!responseList.isEmpty()) {
                    Map<String, Object> translation = responseList.get(0);
                    List<Map<String, String>> translations = (List<Map<String, String>>) translation.get("translations");
                    if (!translations.isEmpty()) {
                        return translations.get(0).get("text");
                    }
                }
            }

            log.warn("Translation failed, returning original text");
            return text;

        } catch (Exception e) {
            log.error("Error during translation", e);
            return text; // Fallback to original text
        }
    }

    /**
     * Detect language of given text
     */
    public String detectLanguage(String text) {
        try {
            // Implementation for language detection
            // For now, return default language
            return "en";
        } catch (Exception e) {
            log.error("Error detecting language", e);
            return "en"; // Default fallback
        }
    }
}
