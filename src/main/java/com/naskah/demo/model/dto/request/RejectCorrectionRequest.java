// ============================================================
// FILE 2: model/dto/request/RejectCorrectionRequest.java
// ============================================================
package com.naskah.demo.model.dto.request;

import lombok.Data;

/**
 * Request body saat admin menolak koreksi.
 * note bersifat opsional — admin bisa isi alasan penolakan.
 */
@Data
public class RejectCorrectionRequest {
    private String note; // opsional
}