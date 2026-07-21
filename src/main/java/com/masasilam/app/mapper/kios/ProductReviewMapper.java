package com.masasilam.app.mapper.kios;

import com.masasilam.app.model.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReviewMapper {
    void insertReview(ProductReview review);
    List<ProductReview> findByProductId(@Param("productId") Long productId, @Param("offset") int offset, @Param("limit") int limit);
    ProductReview findByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}