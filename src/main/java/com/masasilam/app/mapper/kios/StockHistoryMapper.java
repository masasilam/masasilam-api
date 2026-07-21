package com.masasilam.app.mapper.kios;

import com.masasilam.app.model.entity.StockHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockHistoryMapper {
    void insertStockHistory(StockHistory history);
    List<StockHistory> findByProductId(@Param("productId") Long productId, @Param("offset") int offset, @Param("limit") int limit);
}