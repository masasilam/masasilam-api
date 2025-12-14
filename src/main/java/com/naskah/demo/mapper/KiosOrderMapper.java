package com.naskah.demo.mapper;

import com.naskah.demo.model.dto.response.KiosOrderResponse;
import com.naskah.demo.model.dto.response.SalesStatsResponse;
import com.naskah.demo.model.entity.KiosOrder;
import com.naskah.demo.model.entity.OrderItem;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

// ============ KIOS ORDER MAPPER ============
@Mapper
public interface KiosOrderMapper {

    @Insert("INSERT INTO kios_orders (order_number, user_id, status, subtotal, shipping_cost, tax, " +
            "total_amount, payment_method, payment_status, paid_at, shipping_address, shipping_method, " +
            "shipping_tracking_number, shipping_courier, shipped_at, delivered_at, notes, admin_notes, " +
            "created_at, updated_at) VALUES (#{orderNumber}, #{userId}, #{status}, #{subtotal}, " +
            "#{shippingCost}, #{tax}, #{totalAmount}, #{paymentMethod}, #{paymentStatus}, #{paidAt}, " +
            "#{shippingAddress}, #{shippingMethod}, #{shippingTrackingNumber}, #{shippingCourier}, " +
            "#{shippedAt}, #{deliveredAt}, #{notes}, #{adminNotes}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertOrder(KiosOrder order);

    @Insert("INSERT INTO order_items (order_id, product_id, quantity, price, subtotal, created_at) " +
            "VALUES (#{orderId}, #{productId}, #{quantity}, #{price}, #{subtotal}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertOrderItem(OrderItem orderItem);

    @Update("UPDATE kios_orders SET status = #{status}, payment_status = #{paymentStatus}, " +
            "paid_at = #{paidAt}, shipping_tracking_number = #{shippingTrackingNumber}, " +
            "shipping_courier = #{shippingCourier}, shipped_at = #{shippedAt}, " +
            "delivered_at = #{deliveredAt}, admin_notes = #{adminNotes}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    void updateOrder(KiosOrder order);

    @Select("SELECT * FROM kios_orders WHERE order_number = #{orderNumber}")
    KiosOrder findOrderByNumber(String orderNumber);

    KiosOrderResponse getOrderDetailByNumber(String orderNumber);

    List<KiosOrderResponse> findOrdersByUserId(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    int countOrdersByUserId(@Param("userId") Long userId, @Param("status") String status);

    List<KiosOrderResponse> findAllOrdersWithFilters(
            @Param("status") String status,
            @Param("search") String search,
            @Param("offset") int offset,
            @Param("limit") int limit);

    int countAllOrdersWithFilters(@Param("status") String status, @Param("search") String search);

    @Select("SELECT * FROM order_items WHERE order_id = #{orderId}")
    List<OrderItem> findOrderItemsByOrderId(Long orderId);

    @Select("SELECT COUNT(*) > 0 FROM kios_orders o " +
            "JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE o.user_id = #{userId} AND oi.product_id = #{productId} " +
            "AND o.status = 'DELIVERED'")
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    @Select("SELECT COUNT(*) FROM kios_orders")
    Long countAllOrders();

    @Select("SELECT COUNT(*) FROM kios_orders WHERE status = #{status}")
    Long countOrdersByStatus(String status);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM kios_orders WHERE payment_status = 'PAID'")
    BigDecimal calculateTotalRevenue();

    @Select("SELECT COUNT(DISTINCT user_id) FROM kios_orders")
    Long countUniqueCustomers();

    SalesStatsResponse getSalesStatsByPeriod(String period);
}

