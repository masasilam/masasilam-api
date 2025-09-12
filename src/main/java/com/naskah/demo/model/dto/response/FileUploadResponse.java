package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class FileUploadResponse {
    private String originalFilename;
    private String filePath;
    private String fileType;
    private Integer pagesCreated;
    private Long pageId;
    private boolean success;
    private String errorMessage;
    private Double ocrConfidence;
    private String ocrStatus;
}
