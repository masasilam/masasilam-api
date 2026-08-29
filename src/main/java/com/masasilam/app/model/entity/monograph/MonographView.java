package com.masasilam.app.model.entity.monograph;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonographView {
    private Long id;
    private Long monographId;
    private Long userId;
    private String ipAddress;
    private String userAgent;
    private String viewerHash;
}