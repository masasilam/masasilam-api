package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.*;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.service.KiosService;
import com.naskah.demo.model.dto.FileStorageResult;
import com.naskah.demo.util.file.FileUtil;
import com.naskah.demo.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KiosServiceImpl implements KiosService {

    private final KiosProductMapper productMapper;
    private final KiosCartMapper cartMapper;
    private final KiosOrderMapper orderMapper;
    private final KiosWishlistMapper wishlistMapper;
    private final ProductReviewMapper reviewMapper;
    private final StockHistoryMapper stockHistoryMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";

    // ============ PRODUCT MANAGEMENT ============

    @Override
    @Transactional
    public DataResponse<KiosProductResponse> createProduct(KiosProductRequest request, List<MultipartFile> images) {
        // Validate admin/staff permission
        validateAdminAccess();

        // Check duplicate product
        int duplicate = productMapper.countByNameAndSku(request.getName(), request.getSku());
        if (duplicate > 0) {
            throw new DataAlreadyExistsException();
        }

        KiosProduct product = new KiosProduct();
        product.setName(request.getName());
        product.setSlug(FileUtil.sanitizeFilename(request.getName()));
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setProductType(request.getProductType()); // PHYSICAL_BOOK, ACCESSORY, MERCHANDISE
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setDiscountPercentage(calculateDiscountPercentage(request.getPrice(), request.getDiscountPrice()));
        product.setStockQuantity(request.getStockQuantity());
        product.setMinStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : 10);
        product.setWeight(request.getWeight());
        product.setDimensions(request.getDimensions());
        product.setIsActive(true);
        product.setIsFeatured(false);
        product.setViewCount(0);
        product.setSoldCount(0);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        // Handle book relation if it's a physical book
        if ("PHYSICAL_BOOK".equals(request.getProductType()) && request.getBookId() != null) {
            product.setBookId(request.getBookId());
        }

        // Upload images
//            if (images != null && !images.isEmpty()) {
//                // Validate all images first
//                FileUtil.validateProductImages(images);
//
//                // Upload all images
//                List<String> imageUrls = FileUtil.uploadMultipleProductImages(images, request.getName());
//
//                // Set URLs
//                product.setImageUrls(FileUtil.joinImageUrls(imageUrls));
//                product.setThumbnailUrl(imageUrls.get(0)); // First image as thumbnail
//            }

        productMapper.insertProduct(product);

        // Create initial stock history
        createStockHistory(product.getId(), product.getStockQuantity(),
                "INITIAL", "Initial stock", null);

        KiosProductResponse response = productMapper.getProductDetailById(product.getId());
        return new DataResponse<>(SUCCESS, ResponseMessage.DATA_CREATED, HttpStatus.CREATED.value(), response);
    }

    @Override
    public DatatableResponse<KiosProductResponse> getProducts(int page, int limit, String sortField,
                                                              String sortOrder, String search, String category, String productType, Boolean inStock) {
        try {
            Map<String, String> allowedSortFields = new HashMap<>();
            allowedSortFields.put("createdAt", "CREATED_AT");
            allowedSortFields.put("name", "NAME");
            allowedSortFields.put("price", "PRICE");
            allowedSortFields.put("soldCount", "SOLD_COUNT");
            allowedSortFields.put("stockQuantity", "STOCK_QUANTITY");

            String sortColumn = allowedSortFields.getOrDefault(sortField, "CREATED_AT");
            String sortType = "DESC".equals(sortOrder) ? "DESC" : "ASC";
            int offset = (page - 1) * limit;

            List<KiosProductResponse> products = productMapper.getProductsWithFilters(
                    search, category, productType, inStock, offset, limit, sortColumn, sortType);

            int total = productMapper.countProductsWithFilters(search, category, productType, inStock);

            PageDataResponse<KiosProductResponse> pageData = new PageDataResponse<>(page, limit, total, products);
            return new DatatableResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error fetching products", e);
            throw e;
        }
    }

    @Override
    public DataResponse<KiosProductResponse> getProductBySlug(String slug) {
        try {
            KiosProductResponse product = productMapper.getProductDetailBySlug(slug);
            if (product == null) {
                throw new DataNotFoundException();
            }

            // Increment view count
            productMapper.incrementViewCount(product.getId());

            return new DataResponse<>(SUCCESS, ResponseMessage.DATA_FETCHED, HttpStatus.OK.value(), product);

        } catch (Exception e) {
            log.error("Error fetching product by slug: {}", slug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<KiosProductResponse> updateProduct(Long id, KiosProductRequest request, List<MultipartFile> images) {
        try {
            validateAdminAccess();

            KiosProduct existing = productMapper.findProductById(id);
            if (existing == null) {
                throw new DataNotFoundException();
            }

            existing.setName(request.getName());
            existing.setSlug(FileUtil.sanitizeFilename(request.getName()));
            existing.setSku(request.getSku());
            existing.setDescription(request.getDescription());
            existing.setCategory(request.getCategory());
            existing.setPrice(request.getPrice());
            existing.setDiscountPrice(request.getDiscountPrice());
            existing.setDiscountPercentage(calculateDiscountPercentage(request.getPrice(), request.getDiscountPrice()));
            existing.setWeight(request.getWeight());
            existing.setDimensions(request.getDimensions());
            existing.setMinStockLevel(request.getMinStockLevel());
            existing.setUpdatedAt(LocalDateTime.now());

            // Upload new images if provided
            if (images != null && !images.isEmpty()) {
                List<String> imageUrls = new ArrayList<>();
                for (MultipartFile image : images) {
                    FileStorageResult result = FileUtil.saveAndUploadProductImage(image, request.getName());
                    imageUrls.add(result.getCloudUrl());
                }
                existing.setImageUrls(String.join(",", imageUrls));
                existing.setThumbnailUrl(imageUrls.get(0));
            }

            productMapper.updateProduct(existing);

            KiosProductResponse response = productMapper.getProductDetailById(id);
            return new DataResponse<>(SUCCESS, ResponseMessage.DATA_UPDATED, HttpStatus.OK.value(), response);

        } catch (IOException e) {
            throw new RuntimeException("Error updating product", e);
        }
    }

    @Override
    @Transactional
    public DefaultResponse deleteProduct(Long id) {
        try {
            validateAdminAccess();

            KiosProduct product = productMapper.findProductById(id);
            if (product == null) {
                throw new DataNotFoundException();
            }

            // Soft delete
            product.setIsActive(false);
            product.setUpdatedAt(LocalDateTime.now());
            productMapper.updateProduct(product);

            return new DefaultResponse(SUCCESS, ResponseMessage.DATA_DELETED, HttpStatus.OK.value());

        } catch (Exception e) {
            log.error("Error deleting product: {}", id, e);
            throw e;
        }
    }

    // ============ INVENTORY MANAGEMENT ============

    @Override
    @Transactional
    public DataResponse<KiosProductResponse> updateStock(Long id, StockUpdateRequest request) {
        try {
            validateAdminAccess();

            KiosProduct product = productMapper.findProductById(id);
            if (product == null) {
                throw new DataNotFoundException();
            }

            int oldStock = product.getStockQuantity();
            int newStock = oldStock + request.getQuantityChange();

            if (newStock < 0) {
                throw new IllegalArgumentException("Stock cannot be negative");
            }

            product.setStockQuantity(newStock);
            product.setUpdatedAt(LocalDateTime.now());
            productMapper.updateProduct(product);

            // Create stock history
            createStockHistory(id, request.getQuantityChange(),
                    request.getType(), request.getReason(), request.getOrderNumber());

            KiosProductResponse response = productMapper.getProductDetailById(id);
            return new DataResponse<>(SUCCESS, "Stock updated successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating stock for product: {}", id, e);
            throw e;
        }
    }

    @Override
    public DataResponse<List<StockHistoryResponse>> getStockHistory(Long productId, int page, int limit) {
        try {
            int offset = (page - 1) * limit;
            List<StockHistory> history = stockHistoryMapper.findByProductId(productId, offset, limit);

            List<StockHistoryResponse> responses = history.stream()
                    .map(this::mapToStockHistoryResponse)
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Stock history retrieved", HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error fetching stock history for product: {}", productId, e);
            throw e;
        }
    }

    // ============ CART MANAGEMENT ============

    @Override
    @Transactional
    public DataResponse<CartResponse> addToCart(CartItemRequest request) {
        try {
            User user = getCurrentUser();

            KiosProduct product = productMapper.findProductById(request.getProductId());
            if (product == null || !product.getIsActive()) {
                throw new DataNotFoundException();
            }

            if (product.getStockQuantity() < request.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock");
            }

            // Check if item already in cart
            CartItem existing = cartMapper.findByUserAndProduct(user.getId(), request.getProductId());

            if (existing != null) {
                int newQuantity = existing.getQuantity() + request.getQuantity();
                if (newQuantity > product.getStockQuantity()) {
                    throw new IllegalArgumentException("Insufficient stock");
                }
                existing.setQuantity(newQuantity);
                existing.setUpdatedAt(LocalDateTime.now());
                cartMapper.updateCartItem(existing);
            } else {
                CartItem cartItem = new CartItem();
                cartItem.setUserId(user.getId());
                cartItem.setProductId(request.getProductId());
                cartItem.setQuantity(request.getQuantity());
                cartItem.setCreatedAt(LocalDateTime.now());
                cartItem.setUpdatedAt(LocalDateTime.now());
                cartMapper.insertCartItem(cartItem);
            }

            CartResponse response = getCartResponse(user.getId());
            return new DataResponse<>(SUCCESS, "Item added to cart", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error adding to cart", e);
            throw e;
        }
    }

    @Override
    public DataResponse<CartResponse> getCart() {
        try {
            User user = getCurrentUser();
            CartResponse response = getCartResponse(user.getId());
            return new DataResponse<>(SUCCESS, "Cart retrieved", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error fetching cart", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<CartResponse> updateCartItem(Long itemId, UpdateCartItemRequest request) {
        try {
            User user = getCurrentUser();

            CartItem cartItem = cartMapper.findCartItemById(itemId);
            if (cartItem == null || !cartItem.getUserId().equals(user.getId())) {
                throw new DataNotFoundException();
            }

            KiosProduct product = productMapper.findProductById(cartItem.getProductId());
            if (request.getQuantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException("Insufficient stock");
            }

            cartItem.setQuantity(request.getQuantity());
            cartItem.setUpdatedAt(LocalDateTime.now());
            cartMapper.updateCartItem(cartItem);

            CartResponse response = getCartResponse(user.getId());
            return new DataResponse<>(SUCCESS, "Cart updated", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating cart item: {}", itemId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<CartResponse> removeFromCart(Long itemId) {
        try {
            User user = getCurrentUser();

            CartItem cartItem = cartMapper.findCartItemById(itemId);
            if (cartItem == null || !cartItem.getUserId().equals(user.getId())) {
                throw new DataNotFoundException();
            }

            cartMapper.deleteCartItem(itemId);

            CartResponse response = getCartResponse(user.getId());
            return new DataResponse<>(SUCCESS, "Item removed from cart", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error removing cart item: {}", itemId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DefaultResponse clearCart() {
        try {
            User user = getCurrentUser();
            cartMapper.deleteAllByUserId(user.getId());
            return new DefaultResponse(SUCCESS, "Cart cleared", HttpStatus.OK.value());

        } catch (Exception e) {
            log.error("Error clearing cart", e);
            throw e;
        }
    }

    // ============ ORDER MANAGEMENT ============

    @Override
    @Transactional
    public DataResponse<KiosOrderResponse> createOrder(CreateOrderRequest request) {
        try {
            User user = getCurrentUser();

            // Get cart items
            List<CartItem> cartItems = cartMapper.findByUserId(user.getId());
            if (cartItems.isEmpty()) {
                throw new IllegalArgumentException("Cart is empty");
            }

            // Calculate totals
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal totalWeight = BigDecimal.ZERO;

            for (CartItem item : cartItems) {
                KiosProduct product = productMapper.findProductById(item.getProductId());

                // Check stock
                if (product.getStockQuantity() < item.getQuantity()) {
                    throw new IllegalArgumentException("Insufficient stock for: " + product.getName());
                }

                BigDecimal itemPrice = product.getDiscountPrice() != null ?
                        product.getDiscountPrice() : product.getPrice();
                subtotal = subtotal.add(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));

                if (product.getWeight() != null) {
                    totalWeight = totalWeight.add(product.getWeight().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }

            // Calculate shipping (simple calculation based on weight)
            BigDecimal shippingCost = calculateShippingCost(totalWeight, request.getShippingAddress().getProvince());
            BigDecimal total = subtotal.add(shippingCost);

            // Generate order number
            String orderNumber = generateOrderNumber();

            // Create order
            KiosOrder order = new KiosOrder();
            order.setOrderNumber(orderNumber);
            order.setUserId(user.getId());
            order.setStatus("PENDING");
            order.setSubtotal(subtotal);
            order.setShippingCost(shippingCost);
            order.setTotalAmount(total);
            order.setPaymentMethod(request.getPaymentMethod());
            order.setPaymentStatus("UNPAID");
            order.setShippingAddress(formatAddress(request.getShippingAddress()));
            order.setShippingMethod(request.getShippingMethod());
            order.setNotes(request.getNotes());
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            orderMapper.insertOrder(order);

            // Create order items and reduce stock
            for (CartItem item : cartItems) {
                KiosProduct product = productMapper.findProductById(item.getProductId());

                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setProductId(item.getProductId());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setPrice(product.getDiscountPrice() != null ?
                        product.getDiscountPrice() : product.getPrice());
                orderItem.setSubtotal(orderItem.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                orderMapper.insertOrderItem(orderItem);

                // Reduce stock
                product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                product.setSoldCount(product.getSoldCount() + item.getQuantity());
                productMapper.updateProduct(product);

                // Create stock history
                createStockHistory(product.getId(), -item.getQuantity(),
                        "ORDER", "Order created", orderNumber);
            }

            // Clear cart
            cartMapper.deleteAllByUserId(user.getId());

            KiosOrderResponse response = orderMapper.getOrderDetailByNumber(orderNumber);
            return new DataResponse<>(SUCCESS, "Order created successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error creating order", e);
            throw e;
        }
    }

    @Override
    public DatatableResponse<KiosOrderResponse> getUserOrders(int page, int limit, String status) {
        try {
            User user = getCurrentUser();
            int offset = (page - 1) * limit;

            List<KiosOrderResponse> orders = orderMapper.findOrdersByUserId(
                    user.getId(), status, offset, limit);
            int total = orderMapper.countOrdersByUserId(user.getId(), status);

            PageDataResponse<KiosOrderResponse> pageData = new PageDataResponse<>(page, limit, total, orders);
            return new DatatableResponse<>(SUCCESS, "Orders retrieved", HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error fetching user orders", e);
            throw e;
        }
    }

    @Override
    public DataResponse<KiosOrderResponse> getOrderByNumber(String orderNumber) {
        try {
            User user = getCurrentUser();

            KiosOrderResponse order = orderMapper.getOrderDetailByNumber(orderNumber);
            if (order == null) {
                throw new DataNotFoundException();
            }

            // Verify user owns this order
            if (!order.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            return new DataResponse<>(SUCCESS, "Order retrieved", HttpStatus.OK.value(), order);

        } catch (Exception e) {
            log.error("Error fetching order: {}", orderNumber, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<KiosOrderResponse> cancelOrder(String orderNumber) {
        try {
            User user = getCurrentUser();

            KiosOrder order = orderMapper.findOrderByNumber(orderNumber);
            if (order == null) {
                throw new DataNotFoundException();
            }

            if (!order.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            // Only allow cancellation for pending or confirmed orders
            if (!"PENDING".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
                throw new IllegalArgumentException("Cannot cancel order with status: " + order.getStatus());
            }

            order.setStatus("CANCELLED");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateOrder(order);

            // Restore stock
            List<OrderItem> items = orderMapper.findOrderItemsByOrderId(order.getId());
            for (OrderItem item : items) {
                KiosProduct product = productMapper.findProductById(item.getProductId());
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                product.setSoldCount(product.getSoldCount() - item.getQuantity());
                productMapper.updateProduct(product);

                // Create stock history
                createStockHistory(product.getId(), item.getQuantity(),
                        "CANCELLED", "Order cancelled", orderNumber);
            }

            KiosOrderResponse response = orderMapper.getOrderDetailByNumber(orderNumber);
            return new DataResponse<>(SUCCESS, "Order cancelled", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error cancelling order: {}", orderNumber, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<PaymentResponse> processPayment(String orderNumber, PaymentRequest request) {
        try {
            User user = getCurrentUser();

            KiosOrder order = orderMapper.findOrderByNumber(orderNumber);
            if (order == null || !order.getUserId().equals(user.getId())) {
                throw new DataNotFoundException();
            }

            if (!"UNPAID".equals(order.getPaymentStatus())) {
                throw new IllegalArgumentException("Order already paid");
            }

            // Simulate payment processing
            boolean paymentSuccess = simulatePaymentGateway(request);

            PaymentResponse response = new PaymentResponse();
            response.setOrderNumber(orderNumber);
            response.setAmount(order.getTotalAmount());
            response.setPaymentMethod(request.getPaymentMethod());
            response.setPaidAt(LocalDateTime.now());

            if (paymentSuccess) {
                order.setPaymentStatus("PAID");
                order.setStatus("CONFIRMED");
                order.setPaidAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateOrder(order);

                response.setStatus("SUCCESS");
                response.setMessage("Payment successful");
            } else {
                response.setStatus("FAILED");
                response.setMessage("Payment failed");
            }

            return new DataResponse<>(SUCCESS, response.getMessage(), HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error processing payment for order: {}", orderNumber, e);
            throw e;
        }
    }

    // ============ ADMIN ORDER MANAGEMENT ============

    @Override
    public DatatableResponse<KiosOrderResponse> getAllOrders(int page, int limit, String status, String search) {
        try {
            validateAdminAccess();

            int offset = (page - 1) * limit;
            List<KiosOrderResponse> orders = orderMapper.findAllOrdersWithFilters(
                    status, search, offset, limit);
            int total = orderMapper.countAllOrdersWithFilters(status, search);

            PageDataResponse<KiosOrderResponse> pageData = new PageDataResponse<>(page, limit, total, orders);
            return new DatatableResponse<>(SUCCESS, "Orders retrieved", HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error fetching all orders", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<KiosOrderResponse> updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request) {
        try {
            validateAdminAccess();

            KiosOrder order = orderMapper.findOrderByNumber(orderNumber);
            if (order == null) {
                throw new DataNotFoundException();
            }

            order.setStatus(request.getStatus());
            order.setAdminNotes(request.getNotes());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateOrder(order);

            KiosOrderResponse response = orderMapper.getOrderDetailByNumber(orderNumber);
            return new DataResponse<>(SUCCESS, "Order status updated", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating order status: {}", orderNumber, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<KiosOrderResponse> updateShippingInfo(String orderNumber, ShippingUpdateRequest request) {
        try {
            validateAdminAccess();

            KiosOrder order = orderMapper.findOrderByNumber(orderNumber);
            if (order == null) {
                throw new DataNotFoundException();
            }

            order.setShippingTrackingNumber(request.getTrackingNumber());
            order.setShippingCourier(request.getCourier());
            order.setStatus("SHIPPED");
            order.setShippedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateOrder(order);

            KiosOrderResponse response = orderMapper.getOrderDetailByNumber(orderNumber);
            return new DataResponse<>(SUCCESS, "Shipping info updated", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating shipping info: {}", orderNumber, e);
            throw e;
        }
    }

    // ============ WISHLIST ============

    @Override
    @Transactional
    public DataResponse<WishlistResponse> addToWishlist(WishlistRequest request) {
        try {
            User user = getCurrentUser();

            KiosProduct product = productMapper.findProductById(request.getProductId());
            if (product == null) {
                throw new DataNotFoundException();
            }

            // Check if already in wishlist
            Wishlist existing = wishlistMapper.findByUserAndProduct(user.getId(), request.getProductId());
            if (existing != null) {
                throw new DataAlreadyExistsException();
            }

            Wishlist wishlist = new Wishlist();
            wishlist.setUserId(user.getId());
            wishlist.setProductId(request.getProductId());
            wishlist.setCreatedAt(LocalDateTime.now());
            wishlistMapper.insertWishlist(wishlist);

            WishlistResponse response = new WishlistResponse();
            response.setId(wishlist.getId());
            response.setProductId(product.getId());
            response.setProductName(product.getName());
            response.setAddedAt(wishlist.getCreatedAt());

            return new DataResponse<>(SUCCESS, "Added to wishlist", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding to wishlist", e);
            throw e;
        }
    }

    @Override
    public DataResponse<List<KiosProductResponse>> getWishlist() {
        try {
            User user = getCurrentUser();

            List<Wishlist> wishlists = wishlistMapper.findByUserId(user.getId());
            List<KiosProductResponse> products = wishlists.stream()
                    .map(w -> productMapper.getProductDetailById(w.getProductId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Wishlist retrieved", HttpStatus.OK.value(), products);

        } catch (Exception e) {
            log.error("Error fetching wishlist", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DefaultResponse removeFromWishlist(Long productId) {
        try {
            User user = getCurrentUser();

            Wishlist wishlist = wishlistMapper.findByUserAndProduct(user.getId(), productId);
            if (wishlist == null) {
                throw new DataNotFoundException();
            }

            wishlistMapper.deleteWishlist(wishlist.getId());
            return new DefaultResponse(SUCCESS, "Removed from wishlist", HttpStatus.OK.value());

        } catch (Exception e) {
            log.error("Error removing from wishlist", e);
            throw e;
        }
    }

    // ============ REVIEWS ============

    @Override
    @Transactional
    public DataResponse<ProductReviewResponse> addProductReview(Long productId, ProductReviewRequest request) {
        try {
            User user = getCurrentUser();

            KiosProduct product = productMapper.findProductById(productId);
            if (product == null) {
                throw new DataNotFoundException();
            }

            // Check if user has purchased this product
            boolean hasPurchased = orderMapper.hasUserPurchasedProduct(user.getId(), productId);
            if (!hasPurchased) {
                throw new IllegalArgumentException("You can only review products you've purchased");
            }

            // Check if user already reviewed
            ProductReview existing = reviewMapper.findByUserAndProduct(user.getId(), productId);
            if (existing != null) {
                throw new DataAlreadyExistsException();
            }

            ProductReview review = new ProductReview();
            review.setUserId(user.getId());
            review.setProductId(productId);
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());
            reviewMapper.insertReview(review);

            ProductReviewResponse response = mapToReviewResponse(review, user);
            return new DataResponse<>(SUCCESS, "Review added", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding review", e);
            throw e;
        }
    }

    @Override
    public DataResponse<List<ProductReviewResponse>> getProductReviews(Long productId, int page, int limit) {
        try {
            int offset = (page - 1) * limit;
            List<ProductReview> reviews = reviewMapper.findByProductId(productId, offset, limit);

            List<ProductReviewResponse> responses = reviews.stream()
                    .map(review -> {
                        User user = userMapper.findUserById(review.getUserId());
                        return mapToReviewResponse(review, user);
                    })
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Reviews retrieved", HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error fetching reviews", e);
            throw e;
        }
    }

    // ============ STATISTICS ============

    @Override
    public DataResponse<KiosDashboardStats> getDashboardStats() {
        try {
            validateAdminAccess();

            KiosDashboardStats stats = new KiosDashboardStats();
            stats.setTotalProducts(productMapper.countAllProducts());
            stats.setTotalOrders(orderMapper.countAllOrders());
            stats.setTotalRevenue(orderMapper.calculateTotalRevenue());
            stats.setPendingOrders(orderMapper.countOrdersByStatus("PENDING"));
            stats.setLowStockProducts(productMapper.countLowStockProducts());
            stats.setTotalCustomers(orderMapper.countUniqueCustomers());

            return new DataResponse<>(SUCCESS, "Dashboard stats retrieved", HttpStatus.OK.value(), stats);

        } catch (Exception e) {
            log.error("Error fetching dashboard stats", e);
            throw e;
        }
    }

    @Override
    public DataResponse<SalesStatsResponse> getSalesStats(String period) {
        try {
            validateAdminAccess();

            SalesStatsResponse stats = orderMapper.getSalesStatsByPeriod(period);
            return new DataResponse<>(SUCCESS, "Sales stats retrieved", HttpStatus.OK.value(), stats);

        } catch (Exception e) {
            log.error("Error fetching sales stats", e);
            throw e;
        }
    }

    // ============ HELPER METHODS ============

    private User getCurrentUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.isEmpty()) {
            throw new UnauthorizedException();
        }

        User user = userMapper.findUserByUsername(username);
        if (user == null) {
            throw new DataNotFoundException();
        }

        return user;
    }

    private void validateAdminAccess() {
//        User user = getCurrentUser();
//        if (!"ADMIN".equals(user.getRole()) && !"STAFF".equals(user.getRole())) {
//            throw new UnauthorizedException();
//        }
    }

    private BigDecimal calculateDiscountPercentage(BigDecimal price, BigDecimal discountPrice) {
        if (discountPrice == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(discountPrice)
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void createStockHistory(Long productId, Integer quantityChange,
                                    String type, String reason, String orderNumber) {
        StockHistory history = new StockHistory();
        history.setProductId(productId);
        history.setQuantityChange(quantityChange);
        history.setType(type);
        history.setReason(reason);
        history.setOrderNumber(orderNumber);
        history.setCreatedBy(headerHolder.getUsername());
        history.setCreatedAt(LocalDateTime.now());
        stockHistoryMapper.insertStockHistory(history);
    }

    private CartResponse getCartResponse(Long userId) {
        List<CartItem> items = cartMapper.findByUserId(userId);

        List<CartResponse.CartItemResponse> itemResponses = items.stream()
                .map(item -> {
                    KiosProduct product = productMapper.findProductById(item.getProductId());
                    CartResponse.CartItemResponse itemResponse = new CartResponse.CartItemResponse();
                    itemResponse.setId(item.getId());
                    itemResponse.setProductId(product.getId());
                    itemResponse.setProductName(product.getName());
                    itemResponse.setProductSlug(product.getSlug());
                    itemResponse.setThumbnailUrl(product.getThumbnailUrl());
                    itemResponse.setPrice(product.getDiscountPrice() != null ?
                            product.getDiscountPrice() : product.getPrice());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setSubtotal(itemResponse.getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity())));
                    itemResponse.setStockAvailable(product.getStockQuantity());
                    return itemResponse;
                })
                .collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartResponse response = new CartResponse();
        response.setItems(itemResponses);
        response.setTotalItems(items.size());
        response.setTotalAmount(total);

        return response;
    }

    private String generateOrderNumber() {
        return "KIO" + System.currentTimeMillis() +
                String.format("%04d", new Random().nextInt(10000));
    }

    private BigDecimal calculateShippingCost(BigDecimal weight, String province) {
        // Simple shipping calculation
        BigDecimal baseRate = new BigDecimal("15000"); // Base rate
        BigDecimal perKg = new BigDecimal("5000"); // Per kg rate

        BigDecimal weightCost = weight.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP)
                .multiply(perKg);

        return baseRate.add(weightCost);
    }

    private String formatAddress(CreateOrderRequest.ShippingAddress address) {
        return String.format("%s, %s, %s, %s %s, %s",
                address.getStreet(),
                address.getDistrict(),
                address.getCity(),
                address.getProvince(),
                address.getPostalCode(),
                address.getCountry());
    }

    private boolean simulatePaymentGateway(PaymentRequest request) {
        // Simulate payment processing
        // In production, integrate with real payment gateway (Midtrans, Xendit, etc.)
        return true;
    }

    private StockHistoryResponse mapToStockHistoryResponse(StockHistory history) {
        StockHistoryResponse response = new StockHistoryResponse();
        response.setId(history.getId());
        response.setProductId(history.getProductId());
        response.setQuantityChange(history.getQuantityChange());
        response.setType(history.getType());
        response.setReason(history.getReason());
        response.setOrderNumber(history.getOrderNumber());
        response.setCreatedBy(history.getCreatedBy());
        response.setCreatedAt(history.getCreatedAt());
        return response;
    }

    private ProductReviewResponse mapToReviewResponse(ProductReview review, User user) {
        ProductReviewResponse response = new ProductReviewResponse();
        response.setId(review.getId());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setProductId(review.getProductId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }
}