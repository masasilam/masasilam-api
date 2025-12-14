package com.naskah.demo.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

// ============ PRODUCT REQUEST ============
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KiosProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Product type is required")
    @Pattern(regexp = "PHYSICAL_BOOK|ACCESSORY|MERCHANDISE",
            message = "Product type must be PHYSICAL_BOOK, ACCESSORY, or MERCHANDISE")
    private String productType;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Discount price cannot be negative")
    private BigDecimal discountPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Min(value = 1, message = "Minimum stock level must be at least 1")
    private Integer minStockLevel;

    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    private BigDecimal weight;

    @Size(max = 50, message = "Dimensions must not exceed 50 characters")
    private String dimensions;

    private Long bookId; // For physical books - reference to books table
}
