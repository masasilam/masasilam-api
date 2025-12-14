package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Wishlist;
import org.apache.ibatis.annotations.*;

import java.util.List;

// ============ KIOS WISHLIST MAPPER ============
@Mapper
public interface KiosWishlistMapper {

    @Insert("INSERT INTO wishlists (user_id, product_id, created_at) " +
            "VALUES (#{userId}, #{productId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertWishlist(Wishlist wishlist);

    @Delete("DELETE FROM wishlists WHERE id = #{id}")
    void deleteWishlist(Long id);

    @Select("SELECT * FROM wishlists WHERE user_id = #{userId}")
    List<Wishlist> findByUserId(Long userId);

    @Select("SELECT * FROM wishlists WHERE user_id = #{userId} AND product_id = #{productId}")
    Wishlist findByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}