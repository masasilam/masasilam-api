package com.naskah.app.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EpubBookmarkResponse {
    private Long id;
    private String cfi;
    private String label;
    private LocalDateTime createdAt;
}