package com.masasilam.app.mapper.kios;

import com.masasilam.app.model.entity.Wishlist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KiosWishlistMapper {
    void insertWishlist(Wishlist wishlist);
    void deleteWishlist(Long id);
    List<Wishlist> findByUserId(Long userId);
    Wishlist findByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}