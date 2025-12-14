package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingUpdateRequest {

    @NotBlank(message = "Tracking number is required")
    @Size(max = 100, message = "Tracking number must not exceed 100 characters")
    private String trackingNumber;

    @NotBlank(message = "Courier is required")
    @Size(max = 100, message = "Courier must not exceed 100 characters")
    private String courier; // JNE, JNT, SICEPAT, etc.

    @Size(max = 255, message = "Notes must not exceed 255 characters")
    private String notes;
}
