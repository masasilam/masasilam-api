package com.naskah.demo.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// ============ KIOS DASHBOARD STATS ============
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KiosDashboardStats {
    private Long totalProducts;
    private Long activeProducts;
    private Long inactiveProducts;
    private Long totalOrders;
    private Long pendingOrders;
    private Long processingOrders;
    private Long shippedOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal weeklyRevenue;
    private BigDecimal dailyRevenue;
    private Long lowStockProducts;
    private Long outOfStockProducts;
    private Long totalCustomers;
    private Long newCustomersThisMonth;
    private BigDecimal averageOrderValue;
    private Integer averageItemsPerOrder;
    private List<TopSellingProduct> topSellingProducts;
    private List<LowStockProduct> lowStockProductss;
    private RevenueChart revenueChart;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSellingProduct {
        private Long productId;
        private String productName;
        private String productSlug;
        private String thumbnailUrl;
        private Integer soldCount;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockProduct {
        private Long productId;
        private String productName;
        private String productSlug;
        private Integer currentStock;
        private Integer minStockLevel;
        private String status; // LOW_STOCK, OUT_OF_STOCK
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueChart {
        private List<String> labels;
        private List<BigDecimal> data;
        private String period; // daily, weekly, monthly
    }
}