package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.ProductReview;
import org.apache.ibatis.annotations.*;

import java.util.List;

// ============ PRODUCT REVIEW MAPPER ============
@Mapper
public interface ProductReviewMapper {

    @Insert("INSERT INTO product_reviews (user_id, product_id, rating, comment, created_at, updated_at) " +
            "VALUES (#{userId}, #{productId}, #{rating}, #{comment}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertReview(ProductReview review);

    @Update("UPDATE product_reviews SET rating = #{rating}, comment = #{comment}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    void updateReview(ProductReview review);

    @Select("SELECT * FROM product_reviews WHERE id = #{id}")
    ProductReview findReviewById(Long id);

    @Select("SELECT * FROM product_reviews WHERE product_id = #{productId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ProductReview> findByProductId(
            @Param("productId") Long productId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("SELECT * FROM product_reviews WHERE user_id = #{userId} AND product_id = #{productId}")
    ProductReview findByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
