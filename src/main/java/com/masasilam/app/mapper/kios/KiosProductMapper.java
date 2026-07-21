package com.masasilam.app.mapper.kios;

import com.masasilam.app.model.entity.*;
import com.masasilam.app.model.dto.response.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KiosProductMapper {
    void insertProduct(KiosProduct product);
    void updateProduct(KiosProduct product);
    KiosProduct findProductById(Long id);
    KiosProductResponse getProductDetailById(Long id);
    KiosProductResponse getProductDetailBySlug(String slug);
    List<KiosProductResponse> getProductsWithFilters(@Param("search") String search, @Param("category") String category, @Param("productType") String productType, @Param("inStock") Boolean inStock, @Param("offset") int offset, @Param("limit") int limit, @Param("sortColumn") String sortColumn, @Param("sortType") String sortType);
    int countProductsWithFilters(@Param("search") String search, @Param("category") String category, @Param("productType") String productType, @Param("inStock") Boolean inStock);
    int countByNameAndSku(@Param("name") String name, @Param("sku") String sku);
    void incrementViewCount(Long id);
    Long countAllProducts();
    Long countLowStockProducts();
}