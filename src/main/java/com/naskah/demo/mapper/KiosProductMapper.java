package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.*;
import com.naskah.demo.model.dto.response.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

// ============ KIOS PRODUCT MAPPER ============
@Mapper
public interface KiosProductMapper {

    @Insert("INSERT INTO kios_products (name, slug, sku, description, product_type, category, " +
            "price, discount_price, discount_percentage, stock_quantity, min_stock_level, " +
            "weight, dimensions, image_urls, thumbnail_url, book_id, is_active, is_featured, " +
            "view_count, sold_count, created_at, updated_at) " +
            "VALUES (#{name}, #{slug}, #{sku}, #{description}, #{productType}, #{category}, " +
            "#{price}, #{discountPrice}, #{discountPercentage}, #{stockQuantity}, #{minStockLevel}, " +
            "#{weight}, #{dimensions}, #{imageUrls}, #{thumbnailUrl}, #{bookId}, #{isActive}, #{isFeatured}, " +
            "#{viewCount}, #{soldCount}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertProduct(KiosProduct product);

    @Update("UPDATE kios_products SET name = #{name}, slug = #{slug}, sku = #{sku}, " +
            "description = #{description}, category = #{category}, price = #{price}, " +
            "discount_price = #{discountPrice}, discount_percentage = #{discountPercentage}, " +
            "stock_quantity = #{stockQuantity}, min_stock_level = #{minStockLevel}, " +
            "weight = #{weight}, dimensions = #{dimensions}, image_urls = #{imageUrls}, " +
            "thumbnail_url = #{thumbnailUrl}, is_active = #{isActive}, is_featured = #{isFeatured}, " +
            "sold_count = #{soldCount}, updated_at = #{updatedAt} WHERE id = #{id}")
    void updateProduct(KiosProduct product);

    @Select("SELECT * FROM kios_products WHERE id = #{id}")
    KiosProduct findProductById(Long id);

    KiosProductResponse getProductDetailById(Long id);

    KiosProductResponse getProductDetailBySlug(String slug);

    List<KiosProductResponse> getProductsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("productType") String productType,
            @Param("inStock") Boolean inStock,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("sortColumn") String sortColumn,
            @Param("sortType") String sortType);

    int countProductsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("productType") String productType,
            @Param("inStock") Boolean inStock);

    @Select("SELECT COUNT(*) FROM kios_products WHERE name = #{name} AND sku = #{sku}")
    int countByNameAndSku(@Param("name") String name, @Param("sku") String sku);

    @Update("UPDATE kios_products SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(Long id);

    @Select("SELECT COUNT(*) FROM kios_products WHERE is_active = true")
    Long countAllProducts();

    @Select("SELECT COUNT(*) FROM kios_products WHERE stock_quantity <= min_stock_level AND is_active = true")
    Long countLowStockProducts();
}
