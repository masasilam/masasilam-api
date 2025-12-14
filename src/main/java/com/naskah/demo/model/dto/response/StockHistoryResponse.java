package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ============ STOCK HISTORY RESPONSE ============
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockHistoryResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantityChange;
    private Integer stockBefore;
    private Integer stockAfter;
    private String type;
    private String typeLabel;
    private String reason;
    private String orderNumber;
    private String createdBy;
    private LocalDateTime createdAt;
}
