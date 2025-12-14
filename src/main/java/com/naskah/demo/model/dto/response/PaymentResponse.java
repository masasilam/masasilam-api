package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============ PAYMENT RESPONSE ============
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private String orderNumber;
    private BigDecimal amount;
    private String paymentMethod;
    private String status; // SUCCESS, FAILED, PENDING
    private String message;
    private String transactionId;
    private String paymentUrl; // For redirect payments
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
}