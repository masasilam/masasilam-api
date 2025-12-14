package com.naskah.demo.controller;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.KiosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/kios")
@RequiredArgsConstructor
public class KiosController {

    private final KiosService kiosService;

    // ============ PRODUCT MANAGEMENT ============

    @PostMapping("/products")
    public ResponseEntity<DataResponse<KiosProductResponse>> createProduct(
            @Valid @RequestPart("data") KiosProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        log.info("Creating new kios product: {}", request.getName());
        return ResponseEntity.ok(kiosService.createProduct(request, images));
    }

    @GetMapping("/products")
    public ResponseEntity<DatatableResponse<KiosProductResponse>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) Boolean inStock) {
        log.info("Fetching kios products - page: {}, limit: {}", page, limit);
        return ResponseEntity.ok(kiosService.getProducts(page, limit, sortField, sortOrder, search, category, productType, inStock));
    }

    @GetMapping("/products/{slug}")
    public ResponseEntity<DataResponse<KiosProductResponse>> getProductBySlug(@PathVariable String slug) {
        log.info("Fetching product detail: {}", slug);
        return ResponseEntity.ok(kiosService.getProductBySlug(slug));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<DataResponse<KiosProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestPart("data") KiosProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        log.info("Updating product: {}", id);
        return ResponseEntity.ok(kiosService.updateProduct(id, request, images));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<DefaultResponse> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product: {}", id);
        return ResponseEntity.ok(kiosService.deleteProduct(id));
    }

    // ============ INVENTORY MANAGEMENT ============

    @PutMapping("/products/{id}/stock")
    public ResponseEntity<DataResponse<KiosProductResponse>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateRequest request) {
        log.info("Updating stock for product: {}", id);
        return ResponseEntity.ok(kiosService.updateStock(id, request));
    }

    @GetMapping("/products/{id}/stock-history")
    public ResponseEntity<DataResponse<List<StockHistoryResponse>>> getStockHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("Fetching stock history for product: {}", id);
        return ResponseEntity.ok(kiosService.getStockHistory(id, page, limit));
    }

    // ============ CART MANAGEMENT ============

    @PostMapping("/cart/items")
    public ResponseEntity<DataResponse<CartResponse>> addToCart(@Valid @RequestBody CartItemRequest request) {
        log.info("Adding item to cart");
        return ResponseEntity.ok(kiosService.addToCart(request));
    }

    @GetMapping("/cart")
    public ResponseEntity<DataResponse<CartResponse>> getCart() {
        log.info("Fetching user cart");
        return ResponseEntity.ok(kiosService.getCart());
    }

    @PutMapping("/cart/items/{itemId}")
    public ResponseEntity<DataResponse<CartResponse>> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        log.info("Updating cart item: {}", itemId);
        return ResponseEntity.ok(kiosService.updateCartItem(itemId, request));
    }

    @DeleteMapping("/cart/items/{itemId}")
    public ResponseEntity<DataResponse<CartResponse>> removeFromCart(@PathVariable Long itemId) {
        log.info("Removing item from cart: {}", itemId);
        return ResponseEntity.ok(kiosService.removeFromCart(itemId));
    }

    @DeleteMapping("/cart")
    public ResponseEntity<DefaultResponse> clearCart() {
        log.info("Clearing cart");
        return ResponseEntity.ok(kiosService.clearCart());
    }

    // ============ ORDER MANAGEMENT ============

    @PostMapping("/orders")
    public ResponseEntity<DataResponse<KiosOrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Creating new order");
        return ResponseEntity.ok(kiosService.createOrder(request));
    }

    @GetMapping("/orders")
    public ResponseEntity<DatatableResponse<KiosOrderResponse>> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status) {
        log.info("Fetching user orders - page: {}, status: {}", page, status);
        return ResponseEntity.ok(kiosService.getUserOrders(page, limit, status));
    }

    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<DataResponse<KiosOrderResponse>> getOrderByNumber(@PathVariable String orderNumber) {
        log.info("Fetching order: {}", orderNumber);
        return ResponseEntity.ok(kiosService.getOrderByNumber(orderNumber));
    }

    @PutMapping("/orders/{orderNumber}/cancel")
    public ResponseEntity<DataResponse<KiosOrderResponse>> cancelOrder(@PathVariable String orderNumber) {
        log.info("Cancelling order: {}", orderNumber);
        return ResponseEntity.ok(kiosService.cancelOrder(orderNumber));
    }

    @PostMapping("/orders/{orderNumber}/payment")
    public ResponseEntity<DataResponse<PaymentResponse>> processPayment(
            @PathVariable String orderNumber,
            @Valid @RequestBody PaymentRequest request) {
        log.info("Processing payment for order: {}", orderNumber);
        return ResponseEntity.ok(kiosService.processPayment(orderNumber, request));
    }

    // ============ ADMIN - ORDER MANAGEMENT ============

    @GetMapping("/admin/orders")
    public ResponseEntity<DatatableResponse<KiosOrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        log.info("Admin fetching all orders - page: {}, status: {}", page, status);
        return ResponseEntity.ok(kiosService.getAllOrders(page, limit, status, search));
    }

    @PutMapping("/admin/orders/{orderNumber}/status")
    public ResponseEntity<DataResponse<KiosOrderResponse>> updateOrderStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Admin updating order status: {} to {}", orderNumber, request.getStatus());
        return ResponseEntity.ok(kiosService.updateOrderStatus(orderNumber, request));
    }

    @PostMapping("/admin/orders/{orderNumber}/shipping")
    public ResponseEntity<DataResponse<KiosOrderResponse>> updateShippingInfo(
            @PathVariable String orderNumber,
            @Valid @RequestBody ShippingUpdateRequest request) {
        log.info("Admin updating shipping info for order: {}", orderNumber);
        return ResponseEntity.ok(kiosService.updateShippingInfo(orderNumber, request));
    }

    // ============ WISHLIST ============

    @PostMapping("/wishlist")
    public ResponseEntity<DataResponse<WishlistResponse>> addToWishlist(@Valid @RequestBody WishlistRequest request) {
        log.info("Adding product to wishlist: {}", request.getProductId());
        return ResponseEntity.ok(kiosService.addToWishlist(request));
    }

    @GetMapping("/wishlist")
    public ResponseEntity<DataResponse<List<KiosProductResponse>>> getWishlist() {
        log.info("Fetching user wishlist");
        return ResponseEntity.ok(kiosService.getWishlist());
    }

    @DeleteMapping("/wishlist/{productId}")
    public ResponseEntity<DefaultResponse> removeFromWishlist(@PathVariable Long productId) {
        log.info("Removing product from wishlist: {}", productId);
        return ResponseEntity.ok(kiosService.removeFromWishlist(productId));
    }

    // ============ REVIEWS ============

    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<DataResponse<ProductReviewResponse>> addReview(
            @PathVariable Long productId,
            @Valid @RequestBody ProductReviewRequest request) {
        log.info("Adding review for product: {}", productId);
        return ResponseEntity.ok(kiosService.addProductReview(productId, request));
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<DataResponse<List<ProductReviewResponse>>> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("Fetching reviews for product: {}", productId);
        return ResponseEntity.ok(kiosService.getProductReviews(productId, page, limit));
    }

    // ============ STATISTICS ============

    @GetMapping("/stats/dashboard")
    public ResponseEntity<DataResponse<KiosDashboardStats>> getDashboardStats() {
        log.info("Fetching kios dashboard statistics");
        return ResponseEntity.ok(kiosService.getDashboardStats());
    }

    @GetMapping("/stats/sales")
    public ResponseEntity<DataResponse<SalesStatsResponse>> getSalesStats(
            @RequestParam String period) { // daily, weekly, monthly, yearly
        log.info("Fetching sales statistics for period: {}", period);
        return ResponseEntity.ok(kiosService.getSalesStats(period));
    }
}
