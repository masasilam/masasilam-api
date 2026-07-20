package com.masasilam.app.service.common;

import com.masasilam.app.model.dto.request.CartItemRequest;
import com.masasilam.app.model.dto.request.CreateOrderRequest;
import com.masasilam.app.model.dto.request.KiosProductRequest;
import com.masasilam.app.model.dto.request.PaymentRequest;
import com.masasilam.app.model.dto.request.ProductReviewRequest;
import com.masasilam.app.model.dto.request.ShippingUpdateRequest;
import com.masasilam.app.model.dto.request.StockUpdateRequest;
import com.masasilam.app.model.dto.request.UpdateCartItemRequest;
import com.masasilam.app.model.dto.request.UpdateOrderStatusRequest;
import com.masasilam.app.model.dto.request.WishlistRequest;
import com.masasilam.app.model.dto.response.CartResponse;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.DatatableResponse;
import com.masasilam.app.model.dto.response.DefaultResponse;
import com.masasilam.app.model.dto.response.KiosDashboardStats;
import com.masasilam.app.model.dto.response.KiosOrderResponse;
import com.masasilam.app.model.dto.response.KiosProductResponse;
import com.masasilam.app.model.dto.response.PaymentResponse;
import com.masasilam.app.model.dto.response.ProductReviewResponse;
import com.masasilam.app.model.dto.response.SalesStatsResponse;
import com.masasilam.app.model.dto.response.StockHistoryResponse;
import com.masasilam.app.model.dto.response.WishlistResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KiosService {
    DataResponse<KiosProductResponse> createProduct(KiosProductRequest request, List<MultipartFile> images);
    DatatableResponse<KiosProductResponse> getProducts(int page, int limit, String sortField, String sortOrder, String search, String category, String productType, Boolean inStock);
    DataResponse<KiosProductResponse> getProductBySlug(String slug);
    DataResponse<KiosProductResponse> updateProduct(Long id, KiosProductRequest request, List<MultipartFile> images);
    DefaultResponse deleteProduct(Long id);
    DataResponse<KiosProductResponse> updateStock(Long id, StockUpdateRequest request);
    DataResponse<List<StockHistoryResponse>> getStockHistory(Long productId, int page, int limit);
    DataResponse<CartResponse> addToCart(CartItemRequest request);
    DataResponse<CartResponse> getCart();
    DataResponse<CartResponse> updateCartItem(Long itemId, UpdateCartItemRequest request);
    DataResponse<CartResponse> removeFromCart(Long itemId);
    DefaultResponse clearCart();
    DataResponse<KiosOrderResponse> createOrder(CreateOrderRequest request);
    DatatableResponse<KiosOrderResponse> getUserOrders(int page, int limit, String status);
    DataResponse<KiosOrderResponse> getOrderByNumber(String orderNumber);
    DataResponse<KiosOrderResponse> cancelOrder(String orderNumber);
    DataResponse<PaymentResponse> processPayment(String orderNumber, PaymentRequest request);
    DatatableResponse<KiosOrderResponse> getAllOrders(int page, int limit, String status, String search);
    DataResponse<KiosOrderResponse> updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request);
    DataResponse<KiosOrderResponse> updateShippingInfo(String orderNumber, ShippingUpdateRequest request);
    DataResponse<WishlistResponse> addToWishlist(WishlistRequest request);
    DataResponse<List<KiosProductResponse>> getWishlist();
    DefaultResponse removeFromWishlist(Long productId);
    DataResponse<ProductReviewResponse> addProductReview(Long productId, ProductReviewRequest request);
    DataResponse<List<ProductReviewResponse>> getProductReviews(Long productId, int page, int limit);
    DataResponse<KiosDashboardStats> getDashboardStats();
    DataResponse<SalesStatsResponse> getSalesStats(String period);
}