package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.CartItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

// ============ KIOS CART MAPPER ============
@Mapper
public interface KiosCartMapper {

    @Insert("INSERT INTO cart_items (user_id, product_id, quantity, created_at, updated_at) " +
            "VALUES (#{userId}, #{productId}, #{quantity}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertCartItem(CartItem cartItem);

    @Update("UPDATE cart_items SET quantity = #{quantity}, updated_at = #{updatedAt} WHERE id = #{id}")
    void updateCartItem(CartItem cartItem);

    @Delete("DELETE FROM cart_items WHERE id = #{id}")
    void deleteCartItem(Long id);

    @Delete("DELETE FROM cart_items WHERE user_id = #{userId}")
    void deleteAllByUserId(Long userId);

    @Select("SELECT * FROM cart_items WHERE id = #{id}")
    CartItem findCartItemById(Long id);

    @Select("SELECT * FROM cart_items WHERE user_id = #{userId}")
    List<CartItem> findByUserId(Long userId);

    @Select("SELECT * FROM cart_items WHERE user_id = #{userId} AND product_id = #{productId}")
    CartItem findByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
