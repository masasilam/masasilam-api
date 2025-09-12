package com.naskah.demo.service.impl;

import com.naskah.demo.service.OCRService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GoogleVisionOCRService implements OCRService {

    @Value("${google.cloud.vision.api-key:}")
    private String apiKey;

    private double lastConfidence = 0.0;

    @Override
    public String extractText(String imagePath) throws Exception {
        // Implementation for Google Cloud Vision API
        // This would require google-cloud-vision dependency
        log.info("Google Vision OCR not implemented yet for: {}", imagePath);
        throw new UnsupportedOperationException("Google Vision OCR not implemented");
    }

    @Override
    public List<String> extractTextFromPDF(String pdfPath) throws Exception {
        throw new UnsupportedOperationException("Google Vision PDF OCR not implemented");
    }

    @Override
    public double getLastConfidence() {
        return lastConfidence;
    }
}