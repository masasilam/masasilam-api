//package com.naskah.demo.controller;
//
//import com.naskah.demo.service.FileStorageService;
//import com.naskah.demo.service.OCRService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/upload")
//@RequiredArgsConstructor
//public class FileUploadController {
//
//    private final FileStorageService fileStorageService;
//    private final OCRService ocrService;
//
//    @PostMapping("/image")
//    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
//            @RequestParam("file") MultipartFile file) {
//        try {
//            String filePath = fileStorageService.storeImage(file);
//
//            Map<String, String> response = new HashMap<>();
//            response.put("filePath", filePath);
//            response.put("originalName", file.getOriginalFilename());
//
//            return ResponseEntity.ok(ApiResponse.success(response, "Image uploaded successfully"));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to upload image: " + e.getMessage()));
//        }
//    }
//
//    @PostMapping("/document")
//    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDocument(
//            @RequestParam("file") MultipartFile file) {
//        try {
//            String filePath = fileStorageService.storeDocument(file);
//
//            Map<String, String> response = new HashMap<>();
//            response.put("filePath", filePath);
//            response.put("originalName", file.getOriginalFilename());
//
//            return ResponseEntity.ok(ApiResponse.success(response, "Document uploaded successfully"));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to upload document: " + e.getMessage()));
//        }
//    }
//
//    @PostMapping("/ocr")
//    public ResponseEntity<ApiResponse<Map<String, String>>> processOCR(
//            @RequestParam("file") MultipartFile file) {
//        try {
//            String filePath = fileStorageService.storeImage(file);
//            String extractedText = ocrService.extractTextFromImage(filePath);
//
//            Map<String, String> response = new HashMap<>();
//            response.put("filePath", filePath);
//            response.put("extractedText", extractedText);
//            response.put("originalName", file.getOriginalFilename());
//
//            return ResponseEntity.ok(ApiResponse.success(response, "OCR processing completed"));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error("OCR processing failed: " + e.getMessage()));
//        }
//    }
//}
