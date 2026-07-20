package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class BookRequest {
    @NotNull
    private MultipartFile bookFile;
}
