package com.naskah.demo.service;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KiosService {

    // ============ PRODUCT MANAGEMENT ============
    DataResponse<KiosProductResponse> createProduct(KiosProductRequest request, List<MultipartFile> images);

    DatatableResponse<KiosProductResponse> getProducts(int page, int limit, String sortField,
                                                       String sortOrder, String search, String category, String productType, Boolean inStock);

    DataResponse<KiosProductResponse> getProductBySlug(String slug);

    DataResponse<KiosProductResponse> updateProduct(Long id, KiosProductRequest request, List<MultipartFile> images);

    DefaultResponse deleteProduct(Long id);

    // ============ INVENTORY MANAGEMENT ============
    DataResponse<KiosProductResponse> updateStock(Long id, StockUpdateRequest request);

    DataResponse<List<StockHistoryResponse>> getStockHistory(Long productId, int page, int limit);

    // ============ CART MANAGEMENT ============
    DataResponse<CartResponse> addToCart(CartItemRequest request);

    DataResponse<CartResponse> getCart();

    DataResponse<CartResponse> updateCartItem(Long itemId, UpdateCartItemRequest request);

    DataResponse<CartResponse> removeFromCart(Long itemId);

    DefaultResponse clearCart();

    // ============ ORDER MANAGEMENT ============
    DataResponse<KiosOrderResponse> createOrder(CreateOrderRequest request);

    DatatableResponse<KiosOrderResponse> getUserOrders(int page, int limit, String status);

    DataResponse<KiosOrderResponse> getOrderByNumber(String orderNumber);

    DataResponse<KiosOrderResponse> cancelOrder(String orderNumber);

    DataResponse<PaymentResponse> processPayment(String orderNumber, PaymentRequest request);

    // ============ ADMIN - ORDER MANAGEMENT ============
    DatatableResponse<KiosOrderResponse> getAllOrders(int page, int limit, String status, String search);

    DataResponse<KiosOrderResponse> updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request);

    DataResponse<KiosOrderResponse> updateShippingInfo(String orderNumber, ShippingUpdateRequest request);

    // ============ WISHLIST ============
    DataResponse<WishlistResponse> addToWishlist(WishlistRequest request);

    DataResponse<List<KiosProductResponse>> getWishlist();

    DefaultResponse removeFromWishlist(Long productId);

    // ============ REVIEWS ============
    DataResponse<ProductReviewResponse> addProductReview(Long productId, ProductReviewRequest request);

    DataResponse<List<ProductReviewResponse>> getProductReviews(Long productId, int page, int limit);

    // ============ STATISTICS ============
    DataResponse<KiosDashboardStats> getDashboardStats();

    DataResponse<SalesStatsResponse> getSalesStats(String period);
}