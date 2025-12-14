package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookRequest {
    @NotNull
    private MultipartFile bookFile;
}
