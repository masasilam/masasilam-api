package com.naskah.demo.model.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// ============ SALES STATS RESPONSE ============
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesStatsResponse {
    private String period; // daily, weekly, monthly, yearly
    private String startDate;
    private String endDate;
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Integer totalItemsSold;
    private BigDecimal averageOrderValue;
    private BigDecimal growthPercentage;
    private List<DailySales> dailySales;
    private List<TopProduct> topProducts;
    private List<CategorySales> categorySales;
    private List<PaymentMethodStats> paymentMethodStats;
    private List<ShippingMethodStats> shippingMethodStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySales {
        private String date;
        private BigDecimal revenue;
        private Long orderCount;
        private Integer itemsSold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private Long productId;
        private String productName;
        private String productSlug;
        private String thumbnailUrl;
        private String productType;
        private Integer quantitySold;
        private BigDecimal revenue;
        private BigDecimal percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySales {
        private String category;
        private Integer quantitySold;
        private BigDecimal revenue;
        private BigDecimal percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodStats {
        private String paymentMethod;
        private Long orderCount;
        private BigDecimal revenue;
        private BigDecimal percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingMethodStats {
        private String shippingMethod;
        private Long orderCount;
        private BigDecimal revenue;
        private BigDecimal percentage;
    }
}
