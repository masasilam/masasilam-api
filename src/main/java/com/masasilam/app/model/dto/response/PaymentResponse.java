package com.masasilam.app.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private String orderNumber;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String message;
    private String transactionId;
    private String paymentUrl;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
}