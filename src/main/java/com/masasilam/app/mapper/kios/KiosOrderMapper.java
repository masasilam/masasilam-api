package com.masasilam.app.mapper.kios;

import com.masasilam.app.model.dto.response.KiosOrderResponse;
import com.masasilam.app.model.dto.response.SalesStatsResponse;
import com.masasilam.app.model.entity.KiosOrder;
import com.masasilam.app.model.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface KiosOrderMapper {
    void insertOrder(KiosOrder order);
    void insertOrderItem(OrderItem orderItem);
    void updateOrder(KiosOrder order);
    KiosOrder findOrderByNumber(String orderNumber);
    KiosOrderResponse getOrderDetailByNumber(String orderNumber);
    List<KiosOrderResponse> findOrdersByUserId(@Param("userId") Long userId, @Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);
    int countOrdersByUserId(@Param("userId") Long userId, @Param("status") String status);
    List<KiosOrderResponse> findAllOrdersWithFilters(@Param("status") String status, @Param("search") String search, @Param("offset") int offset, @Param("limit") int limit);
    int countAllOrdersWithFilters(@Param("status") String status, @Param("search") String search);
    List<OrderItem> findOrderItemsByOrderId(Long orderId);
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);
    Long countAllOrders();
    Long countOrdersByStatus(String status);
    BigDecimal calculateTotalRevenue();
    Long countUniqueCustomers();
    SalesStatsResponse getSalesStatsByPeriod(String period);
}