package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.order.CreateOrderRequest;
import iuh.fit.laven.dto.request.order.OrderPreviewRequest;
import iuh.fit.laven.dto.request.order.StaffOrderFilterRequest;
import iuh.fit.laven.dto.response.OrderPreviewResponse;
import iuh.fit.laven.dto.response.order.*;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.*;
import iuh.fit.laven.model.enums.*;
import iuh.fit.laven.model.id.ReturnItemId;
import iuh.fit.laven.repository.*;
import iuh.fit.laven.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final ColorStockRepository colorStockRepository;
    private final ShipCostRepository shipCostRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartItemRepository cartItemRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final ReturnItemRepository returnItemRepository;
    private final UserService userService;
    private final PromotionService promotionService;
    private final CancellationRepository cancellationRepository;

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES = List.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.DELIVERED);


    @Value("${payment.vietqr.bank-id}")
    private String vietQrBankId;
    @Value("${payment.vietqr.bank-name}")
    private String vietQrBankName;
    @Value("${payment.vietqr.account-number}")
    private String vietQrAccountNumber;
    @Value("${payment.vietqr.account-name}")
    private String vietQrAccountName;


    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getCustomerOrders(String customerId, OrderStatus orderStatus, int page, int size) {
        if (page < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Số trang không hợp lệ");
        }
        if (size <= 0 || size > 100) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Kích thước trang phải từ 1 đến 100");
        }
        Specification<Order> spec = (root, query, cb) ->
                cb.equal(root.get("customer").get("userId"), customerId);
        if (orderStatus != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("orderStatus"), orderStatus));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findAll(spec, pageable).map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerOrderDetailResponse getCustomerOrderDetail(String orderId, String customerId) {
        Order order = orderRepository.findOrderByOrderIdIsAndCustomer_UserId(orderId, customerId);
        if (order == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng");
        }
        return mapToCustomerOrderDetailResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<StaffOrderSummaryResponse> getStaffOrders(StaffOrderFilterRequest filter) {
        validateStaffOrderFilter(filter);
        Specification<Order> spec = (root, query, cb) -> cb.conjunction();
        if (filter.getOrderStatus() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("orderStatus"), filter.getOrderStatus()));
        }
        if (filter.getPaymentStatus() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("paymentStatus"), filter.getPaymentStatus()));
        }
        if (filter.getPaymentMethod() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("paymentMethod"), filter.getPaymentMethod()));
        }
        if (filter.getOrderId() != null && !filter.getOrderId().isBlank()) {
            String keyword = filter.getOrderId().trim().toLowerCase();
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("orderId")), "%" + keyword + "%"));
        }
        if (filter.getCustomerName() != null && !filter.getCustomerName().isBlank()) {
            String keyword = filter.getCustomerName().trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.like(
                            cb.lower(root.get("customer").get("fullName")), "%" + keyword + "%"));
        }
        if (filter.getCreatedFrom() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
        }
        if (filter.getCreatedTo() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo()));
        }
        if (filter.getMinTotalAmount() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("totalAmount"), filter.getMinTotalAmount()));
        }
        if (filter.getMaxTotalAmount() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("totalAmount"), filter.getMaxTotalAmount()));
        }
        int page = filter.getPage() == null ? 0 : filter.getPage();
        int size = filter.getSize() == null ? 10 : filter.getSize();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findAll(spec, pageable).map(StaffOrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public StaffOrderDetailResponse getStaffOrderDetail(String orderId) {
        Order order = orderRepository.findOrderByOrderIdIs(orderId);
        if (order == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng");
        }
        return mapToStaffOrderDetailResponse(order);
    }

    @Transactional(readOnly = true)
    public PaymentQrResponse getCustomerPaymentQr(String orderId, String customerId) {
        Order order = orderRepository.findOrderByOrderIdIsAndCustomer_UserId(orderId, customerId);
        if (order == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng");
        }
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng không sử dụng phương thức chuyển khoản");
        }
        // Thiết kế i.6: chỉ trả QR khi đơn còn chờ xác nhận và thanh toán PENDING hoặc FAILED (chuyển lại)
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng không còn ở trạng thái chờ xác nhận");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING && order.getPaymentStatus() != PaymentStatus.FAILED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng không còn ở trạng thái chờ thanh toán");
        }
        Integer amount = order.getTotalAmount();
        String transferContent = order.getOrderId();
        String qrCodeUrl = generateVietQrCodeUrl(amount, transferContent);
        return PaymentQrResponse.builder()
                .orderId(order.getOrderId())
                .bankName(vietQrBankName)
                .bankAccount(vietQrAccountNumber)
                .accountName(vietQrAccountName)
                .amount(amount)
                .transferContent(transferContent)
                .qrCodeUrl(qrCodeUrl)
                .paymentStatus(order.getPaymentStatus())
                .build();
    }

    @Transactional
    public PaymentNotifyResponse notifyCustomerPayment(String orderId, String customerId) {
        Order order = orderRepository.findOrderByOrderIdIsAndCustomer_UserId(orderId, customerId);
        if (order == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng");
        }
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng không sử dụng phương thức chuyển khoản");
        }
        // Bảng chuyển trạng thái PaymentStatus: PENDING/FAILED → WAITING chỉ khi đơn đang PENDING
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ báo chuyển khoản cho đơn đang chờ xác nhận");
        }
        if (order.getPaymentStatus() == PaymentStatus.WAITING) {
            return PaymentNotifyResponse.builder()
                    .orderId(order.getOrderId())
                    .paymentStatus(order.getPaymentStatus())
                    .message("Đơn hàng đã được ghi nhận thông báo chuyển khoản")
                    .build();
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING && order.getPaymentStatus() != PaymentStatus.FAILED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng không ở trạng thái cho phép thông báo chuyển khoản");
        }
        order.setPaymentStatus(PaymentStatus.WAITING);
        return PaymentNotifyResponse.builder()
                .orderId(order.getOrderId())
                .paymentStatus(order.getPaymentStatus())
                .message("Đã ghi nhận thông báo chuyển khoản. Vui lòng chờ nhân viên xác nhận.")
                .build();
    }

    @Transactional
    public StaffOrderPaymentResponse updateOrderPayment(String orderId, PaymentStatus newPaymentStatus) {
        if(newPaymentStatus == null){
            throw new AppException(HttpStatus.BAD_REQUEST, "Trạng thái thanh toán không được để trống");
        }
        Order order = orderRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng"));
        PaymentStatus currentStatus = order.getPaymentStatus();
        Integer refundAmount = null;
        String message;
        if (newPaymentStatus == PaymentStatus.PAID) {
            if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
                if (currentStatus != PaymentStatus.WAITING) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Đơn chuyển khoản phải ở trạng thái chờ xác nhận thanh toán");
                }
            } else if (order.getPaymentMethod() == PaymentMethod.COD) {
                if (currentStatus != PaymentStatus.PENDING || order.getOrderStatus() != OrderStatus.DELIVERED) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Đơn COD phải đã giao hàng và đang chờ thanh toán");
                }
            } else {
                throw new AppException(HttpStatus.BAD_REQUEST, "Phương thức thanh toán không hợp lệ");
            }
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            message = "Xác nhận thanh toán thành công";
        }
        else if (newPaymentStatus == PaymentStatus.FAILED) {
            if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER
                    || currentStatus != PaymentStatus.WAITING) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ được xác nhận thất bại cho đơn chuyển khoản đang chờ đối soát");
            }
            order.setPaymentStatus(PaymentStatus.FAILED);
            message = "Đã xác nhận thanh toán thất bại";
        }
        else if (newPaymentStatus == PaymentStatus.REFUNDED) {
            if (currentStatus != PaymentStatus.REFUND_PENDING) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng không ở trạng thái chờ hoàn tiền");
            }
            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                refundAmount = order.getTotalAmount();
            } else if (order.getOrderStatus() == OrderStatus.DELIVERY_FAILED
                    || order.getOrderStatus() == OrderStatus.RETURNED) {
                if (order.getReturnRecord() == null) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Không tìm thấy thông tin hoàn tiền");
                }
                refundAmount = order.getReturnRecord().getRefundAmount();
            } else {
                throw new AppException(HttpStatus.BAD_REQUEST, "Trạng thái đơn hàng không hợp lệ để hoàn tiền");
            }
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setRefundedAt(LocalDateTime.now());
            message = "Xác nhận hoàn tiền thành công";
        }
        else {
            throw new AppException(HttpStatus.BAD_REQUEST, "Trạng thái thanh toán không được hỗ trợ");
        }
        return StaffOrderPaymentResponse.builder()
                .orderId(order.getOrderId())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .refundAmount(refundAmount)
                .paidAt(order.getPaidAt())
                .refundedAt(order.getRefundedAt())
                .message(message)
                .build();
    }

    @Transactional
    public OrderPreviewResponse previewOrder(String customerId, OrderPreviewRequest request) {
        userService.checkCanPlaceOrder(customerId);
        ShipCost shipCost = findShipCost(request.getReceiverCity());
        List<PromotionService.ScopedPromotion> activePromotions = promotionService.loadActiveProductPromotions();

        Set<String> checkedItems = new HashSet<>();
        List<OrderPreviewResponse.Item> previewItems = new ArrayList<>();
        int productTotal = 0;
        for (OrderPreviewRequest.Item item : request.getItems()) {
            checkDuplicateItem(checkedItems, item.getVariantId(), item.getColorId().trim().toUpperCase());
            ColorStock colorStock = colorStockRepository
                    .findByVariant_VariantIdAndColor_ColorId(item.getVariantId(), item.getColorId())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm không có màu này"));
            PricedItem priced = priceItem(colorStock, item.getQuantity(), activePromotions);
            productTotal += priced.subtotal();
            previewItems.add(OrderPreviewResponse.Item.builder()
                    .variantId(priced.variant().getVariantId())
                    .colorId(priced.color().getColorId())
                    .productId(priced.product().getProductId())
                    .productDisplayName(priced.product().getProductDisplayName())
                    .imageUrl(priced.imageUrl())
                    .size(priced.variant().getSize())
                    .colorName(priced.color().getName())
                    .quantity(priced.quantity())
                    .salePrice(priced.salePrice())
                    .productDiscount(priced.productDiscount())
                    .unitPrice(priced.unitPrice())
                    .subtotal(priced.subtotal())
                    .build());
        }

        int shippingFee = shipCost.getPriceShip();
        Promotion orderPromotion = findValidCode(
                request.getOrderPromotionCode(), PromotionApplyType.ORDER, customerId, productTotal);
        Promotion shippingPromotion = findValidCode(
                request.getShippingPromotionCode(), PromotionApplyType.SHIPPING, customerId, productTotal);
        int orderDiscount = promotionService.calculateOrderDiscount(orderPromotion, productTotal);
        int shippingDiscount = promotionService.calculateShippingDiscount(shippingPromotion, shippingFee);
        int discountAmount = orderDiscount + shippingDiscount;
        int totalAmount = productTotal + shippingFee - discountAmount;

        return OrderPreviewResponse.builder()
                .items(previewItems)
                .receiverCity(request.getReceiverCity())
                .paymentMethod(request.getPaymentMethod())
                .orderPromotion(toPromotionResponse(orderPromotion, orderDiscount))
                .shippingPromotion(toPromotionResponse(shippingPromotion, shippingDiscount))
                .productTotal(productTotal)
                .shippingFee(shippingFee)
                .orderDiscount(orderDiscount)
                .shippingDiscount(shippingDiscount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .build();
    }

    /** Quy trình 7 (UC09). Toàn bộ chạy trong một transaction. */
    @Transactional
    public CreateOrderResponse createOrder(String customerId, CreateOrderRequest request) {
        // Bước 2: chặn tài khoản bị khóa / đang pendingLock
        userService.checkCanPlaceOrder(customerId);
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        ShipCost shipCost = findShipCost(request.getReceiverCity());
        List<PromotionService.ScopedPromotion> activePromotions = promotionService.loadActiveProductPromotions();

        // Bước 4: khóa dòng ColorStock, kiểm tra còn bán, tồn kho, maxQuantityPerOrder
        Set<String> checkedItems = new HashSet<>();
        List<PricedItem> pricedItems = new ArrayList<>();
        int productTotal = 0;
        for (CreateOrderRequest.Item item : request.getItems()) {
            checkDuplicateItem(checkedItems, item.getVariantId(), item.getColorId());
            ColorStock colorStock = colorStockRepository
                    .findByVariantAndColorForUpdate(item.getVariantId(), item.getColorId())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm không có biến thể màu sắc này"));
            PricedItem priced = priceItem(colorStock, item.getQuantity(), activePromotions);
            pricedItems.add(priced);
            productTotal += priced.subtotal();
        }

        // Bước 3 + 5: kiểm tra mã (PromotionService khóa dòng Promotion) và tính tiền theo phần g
        int shippingFee = shipCost.getPriceShip();
        Promotion orderPromotion = findValidCode(
                request.getOrderPromotionCode(), PromotionApplyType.ORDER, customerId, productTotal);
        Promotion shippingPromotion = findValidCode(
                request.getShippingPromotionCode(), PromotionApplyType.SHIPPING, customerId, productTotal);
        int orderDiscount = promotionService.calculateOrderDiscount(orderPromotion, productTotal);
        int shippingDiscount = promotionService.calculateShippingDiscount(shippingPromotion, shippingFee);
        int discountAmount = orderDiscount + shippingDiscount;
        int totalAmount = productTotal + shippingFee - discountAmount;

        // Bước 6: tạo Order + OrderDetail, trừ kho, xóa món đã đặt khỏi giỏ
        Order order = new Order();
        String orderId = IdGenerator.nextUnique("ORD", orderRepository::existsById);
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddressLine(request.getReceiverAddressLine());
        order.setReceiverWard(request.getReceiverWard());
        order.setReceiverCity(shipCost.getCity());
        order.setOrderPromotion(orderPromotion);
        order.setShippingPromotion(shippingPromotion);
        order.setProductTotal(productTotal);
        order.setShippingFee(shippingFee);
        order.setOrderDiscount(orderDiscount);
        order.setShippingDiscount(shippingDiscount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);
        order.setNote(request.getNote());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order);

        List<OrderDetail> orderDetails = new ArrayList<>();
        List<CreateOrderResponse.Item> responseItems = new ArrayList<>();
        for (PricedItem priced : pricedItems) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setVariant(priced.variant());
            detail.setColor(priced.color());
            detail.setQuantity(priced.quantity());
            detail.setUnitPrice(priced.unitPrice());
            detail.setPromotion(priced.productPromotion());
            detail.setSubtotal(priced.subtotal());
            orderDetails.add(detail);

            // Dòng ColorStock đã bị khóa ở vòng lặp trên, trừ kho trực tiếp
            ColorStock colorStock = priced.colorStock();
            colorStock.setStock(colorStock.getStock() - priced.quantity());

            cartItemRepository.deleteSelectedItem(
                    customerId, priced.variant().getVariantId(), priced.color().getColorId());

            responseItems.add(CreateOrderResponse.Item.builder()
                    .variantId(priced.variant().getVariantId())
                    .colorId(priced.color().getColorId())
                    .productId(priced.product().getProductId())
                    .productDisplayName(priced.product().getProductDisplayName())
                    .imageUrl(priced.imageUrl())
                    .size(priced.variant().getSize())
                    .colorName(priced.color().getName())
                    .quantity(priced.quantity())
                    .unitPrice(priced.unitPrice())
                    .subtotal(priced.subtotal())
                    .build());
        }
        orderDetailRepository.saveAll(orderDetails);

        // Mã nào đạt maxOrderUsage thì status = false
        promotionService.markUsed(orderPromotion);
        promotionService.markUsed(shippingPromotion);

        // Bước 7: chuyển khoản thì trả link VietQR
        String paymentQrUrl = request.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                ? generateVietQrCodeUrl(totalAmount, orderId)
                : null;

        return CreateOrderResponse.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddressLine(order.getReceiverAddressLine())
                .receiverWard(order.getReceiverWard())
                .receiverCity(order.getReceiverCity())
                .note(order.getNote())
                .items(responseItems)
                .productTotal(productTotal)
                .shippingFee(shippingFee)
                .orderDiscount(orderDiscount)
                .shippingDiscount(shippingDiscount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .paymentQrUrl(paymentQrUrl)
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Transactional
    public void updateOrderStatus(String orderId, OrderStatus newStatus, String trackingCode, String staffId) {
        Order order = orderRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Không tìm thấy đơn hàng"));
        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus == newStatus) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng đang ở trạng thái này");
        }
        // PENDING -> CONFIRMED
        if (currentStatus == OrderStatus.PENDING && newStatus == OrderStatus.CONFIRMED) {
            if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER && order.getPaymentStatus() != PaymentStatus.PAID) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "Đơn hàng chuyển khoản phải được thanh toán trước khi xác nhận"
                );
            }
            // Quy trình 9 bước 1: ghi Staff xác nhận để đối chiếu khi có vấn đề
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên"));
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order.setConfirmedBy(staff);
            order.setConfirmedAt(LocalDateTime.now());
        }
        // CONFIRMED -> SHIPPING
        else if (currentStatus == OrderStatus.CONFIRMED && newStatus == OrderStatus.SHIPPING) {
            if (trackingCode == null || trackingCode.isBlank()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Mã vận đơn không được để trống");
            }
            order.setOrderStatus(OrderStatus.SHIPPING);
            order.setTrackingCode(trackingCode.trim());
            order.setShippingAt(LocalDateTime.now());
        }
        // SHIPPING -> DELIVERED
        else if (currentStatus == OrderStatus.SHIPPING && newStatus == OrderStatus.DELIVERED) {
            order.setOrderStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());
        }
        // SHIPPING -> DELIVERY_FAILED
        else if (currentStatus == OrderStatus.SHIPPING && newStatus == OrderStatus.DELIVERY_FAILED) {
            order.setOrderStatus(OrderStatus.DELIVERY_FAILED);
            handleDeliveryFailed(order);
            orderRepository.saveAndFlush(order);
            String customerId = order.getCustomer().getUserId();
            long failedDeliveryCount = orderRepository
                    .countByCustomer_UserIdAndOrderStatus(customerId, OrderStatus.DELIVERY_FAILED);
            userService.lockIfNeeded(customerId, failedDeliveryCount, hasActiveOrders(customerId));
        }
        // DELIVERED -> COMPLETED
        else if (currentStatus == OrderStatus.DELIVERED
                && newStatus == OrderStatus.COMPLETED) {
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa được xác nhận thanh toán");
            }
            order.setOrderStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());
            String customerId = order.getCustomer().getUserId();
            userService.lockIfPendingAndNoActiveOrders(customerId, hasActiveOrders(customerId));
        }
        else {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Không thể chuyển trạng thái đơn hàng từ " + currentStatus + " sang " + newStatus);
        }
        orderRepository.save(order);
    }

    /** Khách hủy đơn của mình (UC12). */
    @Transactional
    public void cancelCustomerOrder(String customerId, String orderId, String reason) {
        String cleanReason = validateCancelReason(reason);
        Order order = orderRepository
                .findByOrderIdAndCustomer_UserIdForUpdate(orderId, customerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        checkCancellable(order);
        cancelOrder(order, cleanReason, order.getCustomer(), null);
    }

    /** Staff hủy đơn (UC25): hệ thống cấp mã bồi thường cho khách (UC34). */
    @Transactional
    public void cancelStaffOrder(String staffId, String orderId, String reason) {
        String cleanReason = validateCancelReason(reason);
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên"));
        Order order = orderRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        checkCancellable(order);
        Promotion compensationPromotion = promotionService.createCompensationPromotion(
                order.getCustomer(), order.getTotalAmount());
        cancelOrder(order, cleanReason, staff, compensationPromotion);
    }

    /** Một món đã kiểm tra và tính giá theo phần g, dùng chung cho xem trước và đặt hàng. */
    private record PricedItem(ColorStock colorStock, int quantity, Promotion productPromotion,
                              int salePrice, int productDiscount, int unitPrice, int subtotal,
                              String imageUrl) {
        Variant variant() {
            return colorStock.getVariant();
        }
        Product product() {
            return colorStock.getVariant().getProduct();
        }
        Color color() {
            return colorStock.getColor();
        }
    }

    /**
     * Kiểm tra món còn bán (sản phẩm và danh mục status = true), số lượng ≤ tồn kho và ≤ maxQuantityPerOrder,
     * rồi tính unitPrice = salePrice − mức giảm PRODUCT cao nhất (không cộng dồn).
     */
    private PricedItem priceItem(ColorStock colorStock, Integer quantity,
                                 List<PromotionService.ScopedPromotion> activePromotions) {
        Product product = colorStock.getVariant().getProduct();
        if (!product.isStatus() || !product.getCategory().isStatus()) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Sản phẩm " + product.getProductName() + " đã ngừng kinh doanh");
        }
        if (quantity == null || quantity <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Số lượng phải lớn hơn 0");
        }
        if (quantity > colorStock.getStock()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Số lượng vượt quá tồn kho");
        }
        if (quantity > product.getMaxQuantityPerOrder()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vượt quá số lượng tối đa cho phép mỗi đơn");
        }

        int salePrice = product.getSalePrice();
        Optional<PromotionService.AppliedPromotion> best =
                promotionService.findBestProductPromotion(product, activePromotions);
        Promotion productPromotion = best.map(PromotionService.AppliedPromotion::promotion).orElse(null);
        int productDiscount = best.map(PromotionService.AppliedPromotion::discount).orElse(0);
        int unitPrice = salePrice - productDiscount;

        String imageUrl = productImageRepository.findByProductProductId(product.getProductId())
                .stream()
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return new PricedItem(colorStock, quantity, productPromotion,
                salePrice, productDiscount, unitPrice, unitPrice * quantity, imageUrl);
    }

    private void checkDuplicateItem(Set<String> checkedItems, String variantId, String colorId) {
        if (!checkedItems.add(variantId + "-" + colorId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Danh sách sản phẩm có biến thể màu sắc bị trùng");
        }
    }

    /** Tỉnh/thành phải có trong ShipCost, không có thì không giao. */
    private ShipCost findShipCost(String city) {
        if (city == null || city.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tỉnh/thành phố không được rỗng");
        }
        return shipCostRepository.findByCityIgnoreCase(city.trim())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST,
                        "Chưa hỗ trợ giao hàng đến tỉnh/thành phố này"));
    }

    /** Không nhập mã thì null; có mã thì PromotionService kiểm tra đủ điều kiện ở quy trình 7 bước 3. */
    private Promotion findValidCode(String code, PromotionApplyType type, String customerId, int productTotal) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return promotionService.getValidCodeForOrder(code, type, customerId, productTotal);
    }

    private String validateCancelReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lý do hủy đơn không được để trống");
        }
        String cleanReason = reason.trim();
        if (cleanReason.length() > 500) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lý do hủy đơn không được vượt quá 500 ký tự");
        }
        return cleanReason;
    }

    /** Quy trình 10 bước 1: chỉ hủy khi PENDING và không đang WAITING. */
    private void checkCancellable(Order order) {
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ được hủy đơn hàng đang chờ xác nhận");
        }
        if (order.getPaymentStatus() == PaymentStatus.WAITING) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng đang chờ xác nhận thanh toán, không thể hủy");
        }
    }

    /**
     * Quy trình 10 bước 2–6: tạo Cancellation, cộng lại kho, PAID → REFUND_PENDING,
     * khóa khách nếu đang pendingLock và hết đơn hoạt động. Mã giảm giá đã dùng KHÔNG trả lại lượt.
     */
    private void cancelOrder(Order order, String reason, User cancelledBy, Promotion compensationPromotion) {
        for (OrderDetail detail : order.getOrderDetails()) {
            ColorStock stock = colorStockRepository
                    .findByVariantAndColorForUpdate(detail.getVariant().getVariantId(), detail.getColor().getColorId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin tồn kho"));
            stock.setStock(stock.getStock() + detail.getQuantity());
        }

        Cancellation cancellation = Cancellation.builder()
                .order(order)
                .reason(reason)
                .cancelledBy(cancelledBy)
                .cancelledAt(LocalDateTime.now())
                .compensationPromotion(compensationPromotion)
                .build();
        cancellationRepository.save(cancellation);

        order.setOrderStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }
        order.setCancellation(cancellation);
        orderRepository.saveAndFlush(order);

        String customerId = order.getCustomer().getUserId();
        userService.lockIfPendingAndNoActiveOrders(customerId, hasActiveOrders(customerId));
    }

    /** Đơn hoạt động theo quy trình 15: PENDING, CONFIRMED, SHIPPING, DELIVERED. */
    private boolean hasActiveOrders(String customerId) {
        return orderRepository.existsByCustomer_UserIdAndOrderStatusIn(customerId, ACTIVE_ORDER_STATUSES);
    }


    private String generateVietQrCodeUrl(Integer amount, String transferContent) {
        return UriComponentsBuilder
                .fromUriString("https://img.vietqr.io/image/"+ vietQrBankId + "-"
                        + vietQrAccountNumber + "-compact2.png")
                .queryParam("amount", amount)
                .queryParam("addInfo", transferContent)
                .queryParam("accountName", vietQrAccountName)
                .build()
                .encode()
                .toUriString();
    }

    private CustomerOrderDetailResponse.CancellationResponse mapCancellation(Order order) {
        Cancellation cancellation = order.getCancellation();
        if (cancellation == null) {
            return null;
        }
        return CustomerOrderDetailResponse.CancellationResponse.builder()
                .reason(cancellation.getReason())
                .cancelledAt(cancellation.getCancelledAt())
                .compensationPromotionCode(cancellation.getCompensationPromotion().getCode())
                .build();
    }

    private List<CustomerOrderDetailResponse.OrderItemResponse> mapOrderItems(Order order) {
        return order.getOrderDetails()
                .stream()
                .map(orderDetail -> {
                    Variant variant = orderDetail.getVariant();
                    Product product = variant.getProduct();
                    List<ProductImage> images = productImageRepository.findByProductProductId(product.getProductId());
                    String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();
                    Color color = orderDetail.getColor();
                    return CustomerOrderDetailResponse.OrderItemResponse.builder()
                            .productId(product.getProductId())
                            .productName(product.getProductDisplayName())
                            .imageUrl(imageUrl)
                            .variantId(variant.getVariantId())
                            .variantSize(variant.getSize())
                            .colorId(color.getColorId())
                            .colorName(color.getName())
                            .quantity(orderDetail.getQuantity())
                            .unitPrice(orderDetail.getUnitPrice())
                            .subtotal(orderDetail.getSubtotal())
                            .build();
                })
                .toList();
    }

    private CustomerOrderDetailResponse.ReturnRecordResponse mapReturnRecord(Order order) {
        ReturnRecord record = order.getReturnRecord();
        if (record == null) {
            return null;
        }
        return CustomerOrderDetailResponse.ReturnRecordResponse.builder()
                .returnType(record.getReturnType())
                .returnReason(record.getReturnReason())
                .status(record.getStatus())
                .note(record.getNote())
                .rejectionReason(record.getRejectionReason())
                .returnShippingCost(record.getReturnShippingCost())
                .refundAmount(record.getRefundAmount())
                .refundBankName(record.getRefundBankName())
                .refundAccountNumber(record.getRefundAccountNumber())
                .refundAccountName(record.getRefundAccountName())
                .createdAt(record.getCreatedAt())
                .approvedAt(record.getApprovedAt())
                .receivedAt(record.getReceivedAt())
                .build();
    }

    private CustomerOrderDetailResponse mapToCustomerOrderDetailResponse(Order order) {
        return CustomerOrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddressLine(order.getReceiverAddressLine())
                .receiverWard(order.getReceiverWard())
                .receiverCity(order.getReceiverCity())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .paidAt(order.getPaidAt())
                .refundedAt(order.getRefundedAt())
                .productTotal(order.getProductTotal())
                .shippingFee(order.getShippingFee())
                .orderDiscount(order.getOrderDiscount())
                .shippingDiscount(order.getShippingDiscount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .trackingCode(order.getTrackingCode())
                .confirmedAt(order.getConfirmedAt())
                .shippingAt(order.getShippingAt())
                .deliveredAt(order.getDeliveredAt())
                .completedAt(order.getCompletedAt())
                .finalizedAt(order.getFinalizedAt())
                .note(order.getNote())
                .items(mapOrderItems(order))
                .cancellation(mapCancellation(order))
                .returnRecord(mapReturnRecord(order))
                .build();
    }

    private StaffOrderDetailResponse mapToStaffOrderDetailResponse(Order order) {
        User customer = order.getCustomer();
        User confirmedBy = order.getConfirmedBy();
        return StaffOrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .customerId(customer != null ? customer.getUserId() : null)
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddressLine(order.getReceiverAddressLine())
                .receiverWard(order.getReceiverWard())
                .receiverCity(order.getReceiverCity())
                .productTotal(order.getProductTotal())
                .shippingFee(order.getShippingFee())
                .orderDiscount(order.getOrderDiscount())
                .shippingDiscount(order.getShippingDiscount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .note(order.getNote())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .paidAt(order.getPaidAt())
                .refundedAt(order.getRefundedAt())
                .confirmedById(confirmedBy != null ? confirmedBy.getUserId() : null)
                .confirmedByName(confirmedBy != null ? confirmedBy.getFullName() : null)
                .confirmedAt(order.getConfirmedAt())
                .trackingCode(order.getTrackingCode())
                .shippingAt(order.getShippingAt())
                .deliveredAt(order.getDeliveredAt())
                .completedAt(order.getCompletedAt())
                .finalizedAt(order.getFinalizedAt())
                .createdAt(order.getCreatedAt())
                .customer(mapCustomerInfo(customer))
                .orderPromotion(mapPromotionInfo(order.getOrderPromotion()))
                .shippingPromotion(mapPromotionInfo(order.getShippingPromotion()))
                .items(mapOrderItems(order))
                .cancellation(mapCancellation(order))
                .returnRecord(mapReturnRecord(order))
                .build();
    }

    private StaffOrderDetailResponse.CustomerInfo mapCustomerInfo(User customer) {
        if (customer == null) {
            return null;
        }
        return StaffOrderDetailResponse.CustomerInfo.builder()
                .userId(customer.getUserId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .build();
    }

    private StaffOrderDetailResponse.PromotionInfo mapPromotionInfo(Promotion promotion) {
        if (promotion == null) {
            return null;
        }
        return StaffOrderDetailResponse.PromotionInfo.builder()
                .promotionId(promotion.getPromotionId())
                .promotionCode(promotion.getCode())
                .promotionName(promotion.getName())
                .build();
    }

    private void validateStaffOrderFilter(StaffOrderFilterRequest filter) {

        if (filter.getCreatedFrom() != null && filter.getCreatedTo() != null
                && filter.getCreatedFrom().isAfter(filter.getCreatedTo())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }

        if (filter.getMinTotalAmount() != null && filter.getMinTotalAmount() < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tổng tiền tối thiểu không hợp lệ");
        }

        if (filter.getMaxTotalAmount() != null && filter.getMaxTotalAmount() < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tổng tiền tối đa không hợp lệ");
        }

        if (filter.getMinTotalAmount() != null && filter.getMaxTotalAmount() != null
                && filter.getMinTotalAmount() > filter.getMaxTotalAmount()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tổng tiền tối thiểu không được lớn hơn tổng tiền tối đa");
        }

        if (filter.getPage() != null && filter.getPage() < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Số trang không hợp lệ");
        }

        if (filter.getSize() != null && (filter.getSize() <= 0 || filter.getSize() > 100)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Kích thước trang phải từ 1 đến 100");
        }
    }

    private OrderPreviewResponse.Promotion toPromotionResponse(Promotion promotion, int discountAmount) {
        if (promotion == null) {
            return null;
        }
        return OrderPreviewResponse.Promotion.builder()
                .promotionCode(promotion.getCode())
                .promotionName(promotion.getName())
                .discountAmount(discountAmount)
                .build();
    }

    private void handleDeliveryFailed(Order order) {
        String orderId = order.getOrderId();
        if (returnRecordRepository.existsById(orderId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng đã có phiếu hoàn hàng");
        }
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng không có sản phẩm");
        }
        // Phần g: returnShippingCost = priceShip của receiverCity tại lúc tạo phiếu (LAVEN chịu)
        ShipCost shipCost = shipCostRepository
                .findByCityIgnoreCase(order.getReceiverCity())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phí vận chuyển của tỉnh/thành người nhận"));
        int returnShippingCost = shipCost.getPriceShip();

        // Phần g: refundAmount = totalAmount nếu đơn đã PAID, ngược lại 0.
        // Phải tính trước khi đổi paymentStatus sang REFUND_PENDING.
        boolean paid = order.getPaymentStatus() == PaymentStatus.PAID;
        int refundAmount = paid ? order.getTotalAmount() : 0;
        if (paid) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }
        ReturnRecord returnRecord = ReturnRecord.builder()
                .order(order)
                .returnType(ReturnType.FAILED_DELIVERY)
                .status(ReturnStatus.IN_TRANSIT)
                .note("Đơn hàng giao thất bại, hàng đang được hoàn về cửa hàng")
                .returnShippingCost(returnShippingCost)
                .refundAmount(refundAmount)
                .createdAt(LocalDateTime.now())
                .build();
        returnRecordRepository.save(returnRecord);
        List<ReturnItem> returnItems = new ArrayList<>();
        for (OrderDetail detail : order.getOrderDetails()) {
            ReturnItemId returnItemId = new ReturnItemId(
                    orderId,
                    detail.getVariant().getVariantId(),
                    detail.getColor().getColorId()
            );
            ReturnItem returnItem = ReturnItem.builder()
                    .id(returnItemId)
                    .order(order)
                    .variant(detail.getVariant())
                    .color(detail.getColor())
                    .quantity(detail.getQuantity())
                    .damagedQuantity(0)
                    .build();
            returnItems.add(returnItem);
        }
        returnItemRepository.saveAll(returnItems);
    }



}
