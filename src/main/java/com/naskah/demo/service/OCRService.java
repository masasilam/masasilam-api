package com.naskah.demo.service;

import java.util.List;

public interface OCRService {
    String extractText(String imagePath) throws Exception;
    List<String> extractTextFromPDF(String pdfPath) throws Exception;
    double getLastConfidence();
}