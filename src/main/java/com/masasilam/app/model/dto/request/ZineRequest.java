package com.masasilam.app.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ZineRequest {
    @NotNull(message = "File zine tidak boleh kosong")
    private MultipartFile zineFile;
}