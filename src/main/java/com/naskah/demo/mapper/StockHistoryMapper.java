package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.StockHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

// ============ STOCK HISTORY MAPPER ============
@Mapper
public interface StockHistoryMapper {

    @Insert("INSERT INTO stock_history (product_id, quantity_change, type, reason, order_number, " +
            "created_by, created_at) VALUES (#{productId}, #{quantityChange}, #{type}, #{reason}, " +
            "#{orderNumber}, #{createdBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertStockHistory(StockHistory history);

    @Select("SELECT * FROM stock_history WHERE product_id = #{productId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<StockHistory> findByProductId(
            @Param("productId") Long productId,
            @Param("offset") int offset,
            @Param("limit") int limit);
}