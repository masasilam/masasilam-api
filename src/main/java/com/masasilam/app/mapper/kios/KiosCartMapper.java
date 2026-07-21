package com.masasilam.app.mapper.kios;

import com.masasilam.app.model.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KiosCartMapper {
    void insertCartItem(CartItem cartItem);
    void updateCartItem(CartItem cartItem);
    void deleteCartItem(Long id);
    void deleteAllByUserId(Long userId);
    CartItem findCartItemById(Long id);
    List<CartItem> findByUserId(Long userId);
    CartItem findByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}