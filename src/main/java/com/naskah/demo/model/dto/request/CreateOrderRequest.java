package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============ CREATE ORDER REQUEST ============
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Payment method is required")
    @Size(max = 50, message = "Payment method must not exceed 50 characters")
    private String paymentMethod; // BANK_TRANSFER, E_WALLET, COD, CREDIT_CARD

    @NotBlank(message = "Shipping method is required")
    @Size(max = 100, message = "Shipping method must not exceed 100 characters")
    private String shippingMethod; // JNE, JNT, SICEPAT, GOJEK, GRAB

    @NotNull(message = "Shipping address is required")
    private ShippingAddress shippingAddress;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddress {

        @NotBlank(message = "Recipient name is required")
        @Size(max = 100, message = "Recipient name must not exceed 100 characters")
        private String recipientName;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^(\\+62|62|0)[0-9]{9,13}$",
                message = "Phone number must be valid Indonesian format")
        private String phoneNumber;

        @NotBlank(message = "Street address is required")
        @Size(max = 255, message = "Street address must not exceed 255 characters")
        private String street;

        @NotBlank(message = "District is required")
        @Size(max = 100, message = "District must not exceed 100 characters")
        private String district;

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        private String city;

        @NotBlank(message = "Province is required")
        @Size(max = 100, message = "Province must not exceed 100 characters")
        private String province;

        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "^[0-9]{5}$", message = "Postal code must be 5 digits")
        private String postalCode;

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        private String country;
    }
}