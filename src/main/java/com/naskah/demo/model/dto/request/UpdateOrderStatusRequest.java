package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============ UPDATE ORDER STATUS REQUEST ============
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "PENDING|CONFIRMED|PROCESSING|SHIPPED|DELIVERED|CANCELLED",
            message = "Status must be PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, or CANCELLED")
    private String status;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}