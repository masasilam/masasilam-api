//package com.naskah.demo.service.impl;
//
//import com.naskah.demo.service.OCRService;
//import lombok.extern.slf4j.Slf4j;
//import net.sourceforge.tess4j.Tesseract;
//import net.sourceforge.tess4j.TesseractException;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.rendering.ImageType;
//import org.apache.pdfbox.rendering.PDFRenderer;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//@Slf4j
//public class TesseractOCRService implements OCRService {
//
//    private final Tesseract tesseract;
//    private double lastConfidence = 0.0;
//
//    @Value("${ocr.tesseract.datapath:/usr/share/tesseract-ocr/4.00/tessdata}")
//    private String tessDataPath;
//
//    @Value("${ocr.tesseract.language:ind+eng}")
//    private String ocrLanguage;
//
//    public TesseractOCRService() {
//        this.tesseract = new Tesseract();
//        initializeTesseract();
//    }
//
//    private void initializeTesseract() {
//        try {
//            tesseract.setDatapath(tessDataPath);
//            tesseract.setLanguage(ocrLanguage);
//            tesseract.setPageSegMode(1); // PSM_AUTO_OSD
//            tesseract.setOcrEngineMode(1); // OEM_LSTM_ONLY
//
//            // Additional configurations for better accuracy
//            tesseract.setVariable("tessedit_char_whitelist",
//                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,;:!?'-\" \n\t");
//
//            log.info("Tesseract OCR initialized with language: {}", ocrLanguage);
//        } catch (Exception e) {
//            log.error("Failed to initialize Tesseract OCR", e);
//            throw new RuntimeException("OCR initialization failed", e);
//        }
//    }
//
//    @Override
//    public String extractText(String imagePath) throws Exception {
//        try {
//            log.info("Starting OCR processing for: {}", imagePath);
//
//            File imageFile = new File(imagePath);
//            if (!imageFile.exists()) {
//                throw new IOException("Image file not found: " + imagePath);
//            }
//
//            // Read image
//            BufferedImage image = ImageIO.read(imageFile);
//            if (image == null) {
//                throw new IOException("Unable to read image file: " + imagePath);
//            }
//
//            // Preprocess image for better OCR results
//            BufferedImage preprocessedImage = preprocessImage(image);
//
//            // Perform OCR
//            String result = tesseract.doOCR(preprocessedImage);
//
//            // Calculate confidence (simplified approach)
//            this.lastConfidence = calculateConfidence(result);
//
//            log.info("OCR completed for: {} with confidence: {}", imagePath, lastConfidence);
//
//            return cleanText(result);
//
//        } catch (TesseractException e) {
//            log.error("Tesseract OCR failed for: " + imagePath, e);
//            throw new Exception("OCR processing failed: " + e.getMessage(), e);
//        } catch (IOException e) {
//            log.error("Image processing failed for: " + imagePath, e);
//            throw new Exception("Image processing failed: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public List<String> extractTextFromPDF(String pdfPath) throws Exception {
//        List<String> extractedTexts = new ArrayList<>();
//
//        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
//            PDFRenderer pdfRenderer = new PDFRenderer(document);
//
//            for (int page = 0; page < document.getNumberOfPages(); page++) {
//                log.info("Processing PDF page: {} of {}", page + 1, document.getNumberOfPages());
//
//                // Convert PDF page to image
//                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
//
//                // Preprocess image
//                BufferedImage preprocessedImage = preprocessImage(pageImage);
//
//                // Perform OCR on the page
//                String pageText = tesseract.doOCR(preprocessedImage);
//                extractedTexts.add(cleanText(pageText));
//            }
//
//        } catch (IOException e) {
//            log.error("PDF processing failed for: " + pdfPath, e);
//            throw new Exception("PDF processing failed: " + e.getMessage(), e);
//        } catch (TesseractException e) {
//            log.error("OCR processing failed for PDF: " + pdfPath, e);
//            throw new Exception("OCR processing failed: " + e.getMessage(), e);
//        }
//
//        return extractedTexts;
//    }
//
//    private BufferedImage preprocessImage(BufferedImage image) {
//        // Convert to grayscale
//        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
//        grayImage.getGraphics().drawImage(image, 0, 0, null);
//
//        // Additional preprocessing can be added here:
//        // - Noise reduction
//        // - Contrast enhancement
//        // - Deskewing
//        // - Border removal
//
//        return grayImage;
//    }
//
//    private String cleanText(String rawText) {
//        if (rawText == null) {
//            return "";
//        }
//
//        return rawText
//                .trim()
//                .replaceAll("\\s+", " ") // Replace multiple spaces with single space
//                .replaceAll("[\r\n]+", "\n") // Normalize line breaks
//                .replaceAll("^\\s+|\\s+$", ""); // Trim whitespace
//    }
//
//    private double calculateConfidence(String text) {
//        // Simplified confidence calculation
//        // In a real implementation, you might use Tesseract's confidence API
//        if (text == null || text.trim().isEmpty()) {
//            return 0.0;
//        }
//
//        // Basic heuristics for confidence
//        double confidence = 80.0; // Base confidence
//
//        // Reduce confidence for very short texts
//        if (text.length() < 10) {
//            confidence -= 20;
//        }
//
//        // Reduce confidence for texts with many special characters
//        long specialCharCount = text.chars()
//                .filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))
//                .count();
//
//        double specialCharRatio = (double) specialCharCount / text.length();
//        if (specialCharRatio > 0.1) {
//            confidence -= (specialCharRatio * 50);
//        }
//
//        return Math.max(0.0, Math.min(100.0, confidence));
//    }
//
//    @Override
//    public double getLastConfidence() {
//        return lastConfidence;
//    }
//}
