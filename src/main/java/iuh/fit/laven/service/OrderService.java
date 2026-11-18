package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.order.CreateOrderRequest;
import iuh.fit.laven.dto.request.order.OrderPreviewRequest;
import iuh.fit.laven.dto.request.order.StaffOrderFilterRequest;
import iuh.fit.laven.dto.response.OrderPreviewResponse;
import iuh.fit.laven.dto.response.order.*;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.*;
import iuh.fit.laven.model.enums.*;
import iuh.fit.laven.repository.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final VariantRepository variantRepository;
    private final ColorStockRepository colorStockRepository;
    private final ShipCostRepository shipCostRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionCategoryRepository promotionCategoryRepository;
    private final PromotionBrandRepository promotionBrandRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartItemRepository cartItemRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final ReturnItemRepository returnItemRepository;
    private final UserService userService;
    private final PromotionService promotionService;
    private final CancellationRepository cancellationRepository;


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
        if (order.getPaymentStatus() != PaymentStatus.PENDING && order.getPaymentStatus() != PaymentStatus.WAITING) {
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
        Order order = orderRepository.findById(orderId)
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

    @Transactional(readOnly = true)
    public OrderPreviewResponse previewOrder(String customerId, OrderPreviewRequest request) {
        User customer = userRepository.findByUserId(customerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        if (customer.isPendingLock()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tài khoản đang chờ khóa, không thể đặt đơn mới");
        }
        ShipCost shipCost = shipCostRepository
                .findByCityIgnoreCase(request.getReceiverCity().trim())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Khu vực giao hàng không được hỗ trợ"));

        List<OrderPreviewResponse.Item> previewItems = new ArrayList<>();
        int productTotal = 0;
        for (OrderPreviewRequest.Item item : request.getItems()) {
            Variant variant = variantRepository
                    .findByVariantId(item.getVariantId())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Không tìm thấy biến thể sản phẩm"));
            Product product = variant.getProduct();
            if (!product.isStatus()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm hiện không còn kinh doanh");
            }
            ColorStock colorStock = colorStockRepository
                    .findByVariant_VariantIdAndColor_ColorId(item.getVariantId(), item.getColorId())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm không có màu này"));
            if (item.getQuantity() > colorStock.getStock()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Số lượng vượt quá tồn kho");
            }
            if (item.getQuantity() > product.getMaxQuantityPerOrder()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Vượt quá số lượng tối đa cho phép mỗi đơn");
            }
            int salePrice = product.getSalePrice();
            Promotion bestProductPromotion = findBestProductPromotion(product, customer);
            int productDiscount = 0;

            if (bestProductPromotion != null) {
                productDiscount = calculateDiscount(bestProductPromotion, product.getSalePrice());
            }
            int unitPrice = product.getSalePrice() - productDiscount;
            int subtotal = unitPrice * item.getQuantity();
            productTotal += subtotal;
            List<ProductImage> images = productImageRepository.findByProductProductId(product.getProductId());
            String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();
            OrderPreviewResponse.Item previewItem = OrderPreviewResponse.Item.builder()
                    .variantId(variant.getVariantId())
                    .colorId(colorStock.getColor().getColorId())
                    .productId(product.getProductId())
                    .productDisplayName(product.getProductDisplayName())
                    .imageUrl(imageUrl)
                    .size(variant.getSize())
                    .colorName(colorStock.getColor().getName())
                    .quantity(item.getQuantity())
                    .originalPrice(product.getOriginalPrice())
                    .salePrice(salePrice)
                    .productDiscount(productDiscount)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();
            previewItems.add(previewItem);
        }
        int shippingFee = shipCost.getPriceShip();
        Promotion orderPromotion = validateOrderPromotion(request.getOrderPromotionCode(), customer, productTotal);
        int orderDiscount = orderPromotion == null ? 0 : calculateDiscount(orderPromotion, productTotal);
        Promotion shippingPromotion = validateShippingPromotion(request.getShippingPromotionCode(), customer, productTotal);
        int shippingDiscount = shippingPromotion == null ? 0 : calculateDiscount(shippingPromotion, shippingFee);
        shippingDiscount = Math.min(shippingDiscount, shippingFee);
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

    @Transactional
    public CreateOrderResponse createOrder(String customerId, CreateOrderRequest request) {
        LocalDate today = LocalDate.now();
        userService.checkCanPlaceOrder(customerId);
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        ShipCost shipCost = shipCostRepository
                .findByCityIgnoreCase(request.getReceiverCity())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Không hỗ trợ giao hàng đến tỉnh/thành phố này"));
        List<OrderDetail> orderDetails = new ArrayList<>();
        List<CreateOrderResponse.Item> responseItems = new ArrayList<>();
        Set<String> checkedItems = new HashSet<>();
        int productTotal = 0;
        for (CreateOrderRequest.Item item : request.getItems()) {
            String itemKey = item.getVariantId() + "-" + item.getColorId();
            if (!checkedItems.add(itemKey)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Danh sách sản phẩm có biến thể màu sắc bị trùng");
            }
            ColorStock colorStock = colorStockRepository
                    .findByVariantAndColorForUpdate(item.getVariantId(), item.getColorId())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm không có biến thể màu sắc này"));
            Variant variant = colorStock.getVariant();
            Product product = variant.getProduct();
            Category category = product.getCategory();
            if (!product.isStatus()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm " + product.getProductName() + " đã ngừng kinh doanh");
            }
            if (!category.isStatus()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Danh mục sản phẩm đã ngừng kinh doanh");
            }
            if (item.getQuantity() > colorStock.getStock()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm không đủ số lượng tồn kho");
            }
            if (item.getQuantity() > product.getMaxQuantityPerOrder()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm vượt quá giới hạn cho phép");
            }
            int salePrice = product.getSalePrice();
            Promotion bestProductPromotion = findBestProductPromotion(product, customer);
            int productDiscount = 0;
            if (bestProductPromotion != null) {
                productDiscount = calculateDiscount(bestProductPromotion, product.getSalePrice());
            }
            int unitPrice = product.getSalePrice() - productDiscount;
            int subtotal = unitPrice * item.getQuantity();
            productTotal += subtotal;
            OrderDetail detail = new OrderDetail();
            detail.setVariant(variant);
            detail.setColor(colorStock.getColor());
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(unitPrice);
            detail.setPromotion(bestProductPromotion);
            detail.setSubtotal(subtotal);
            orderDetails.add(detail);
            String imageUrl = productImageRepository
                    .findByProductProductId(product.getProductId())
                    .stream()
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(null);
            String productDisplayName = product.getProductDisplayName();
            responseItems.add(
                    CreateOrderResponse.Item.builder()
                            .variantId(variant.getVariantId())
                            .colorId(colorStock.getColor().getColorId())
                            .productId(product.getProductId())
                            .productDisplayName(productDisplayName)
                            .imageUrl(imageUrl)
                            .size(variant.getSize())
                            .colorName(colorStock.getColor().getName())
                            .quantity(item.getQuantity())
                            .unitPrice(unitPrice)
                            .subtotal(subtotal)
                            .build()
            );
        }
        Promotion orderPromotion = null;
        int orderDiscount = 0;
        if (request.getOrderPromotionCode() != null && !request.getOrderPromotionCode().isBlank()) {
            orderPromotion = promotionRepository
                    .findByCodeForUpdate(request.getOrderPromotionCode())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Mã giảm giá đơn hàng không tồn tại"));
            if (orderPromotion.getApplyType() != PromotionApplyType.ORDER
                    || !orderPromotion.isStatus()
                    || today.isBefore(orderPromotion.getStartDate())
                    || today.isAfter(orderPromotion.getEndDate())
                    || productTotal < orderPromotion.getMinOrderValue()
                    || (orderPromotion.getCustomer() != null
                        && !orderPromotion.getCustomer().getUserId().equals(customerId))) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Mã giảm giá đơn hàng không hợp lệ");
            }
            long usedCount = orderRepository.countByCustomer_UserIdAndOrderPromotion_PromotionId(
                    customerId, orderPromotion.getPromotionId());
            if (usedCount >= orderPromotion.getMaxOrderUsage()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Bạn đã sử dụng mã giảm giá này hoặc mã đã hết lượt");
            }
            if (orderPromotion.getDiscountType() == DiscountType.PERCENT) {
                orderDiscount = productTotal * orderPromotion.getDiscountValue() / 100;
                if (orderPromotion.getMaxDiscountValue() > 0) {
                    orderDiscount = Math.min(orderDiscount, orderPromotion.getMaxDiscountValue());
                }
            } else {
                orderDiscount = Math.min(orderPromotion.getDiscountValue(), productTotal);
            }
        }
        Promotion shippingPromotion = null;
        int shippingFee = shipCost.getPriceShip();
        int shippingDiscount = 0;
        if (request.getShippingPromotionCode() != null && !request.getShippingPromotionCode().isBlank()) {
            shippingPromotion = promotionRepository
                    .findByCodeForUpdate(request.getShippingPromotionCode())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Mã giảm phí vận chuyển không tồn tại"));
            if (shippingPromotion.getApplyType() != PromotionApplyType.SHIPPING
                    || !shippingPromotion.isStatus()
                    || today.isBefore(shippingPromotion.getStartDate())
                    || today.isAfter(shippingPromotion.getEndDate())
                    || productTotal < shippingPromotion.getMinOrderValue()
                    || (shippingPromotion.getCustomer() != null
                        && !shippingPromotion.getCustomer().getUserId().equals(customerId))) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Mã giảm phí vận chuyển không hợp lệ");
            }
            long usedCount = orderRepository
                    .countByCustomer_UserIdAndShippingPromotion_PromotionId(
                            customerId, shippingPromotion.getPromotionId());
            if (usedCount >= shippingPromotion.getMaxOrderUsage()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Bạn đã sử dụng mã giảm phí vận chuyển này hoặc mã đã hết lượt");
            }
            if (shippingPromotion.getDiscountType() == DiscountType.PERCENT) {
                shippingDiscount = shippingFee * shippingPromotion.getDiscountValue() / 100;
                if (shippingPromotion.getMaxDiscountValue() > 0) {
                    shippingDiscount = Math.min(shippingDiscount, shippingPromotion.getMaxDiscountValue()
                    );
                }
            } else {
                shippingDiscount = shippingPromotion.getDiscountValue();
            }
            shippingDiscount = Math.min(shippingDiscount, shippingFee);
        }
        int discountAmount = orderDiscount + shippingDiscount;
        int totalAmount = productTotal + shippingFee - discountAmount;
        Order order = new Order();
        String orderId = UUID.randomUUID().toString();
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddressLine(request.getReceiverAddressLine());
        order.setReceiverWard(request.getReceiverWard());
        order.setReceiverCity(request.getReceiverCity());
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
        for (OrderDetail detail : orderDetails) {
            detail.setOrder(order);
        }
        orderDetailRepository.saveAll(orderDetails);
        for (CreateOrderRequest.Item item : request.getItems()) {
            ColorStock colorStock = colorStockRepository
                    .findByVariantAndColorForUpdate(item.getVariantId(), item.getColorId())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Không tìm thấy thông tin tồn kho"));
            colorStock.setStock(colorStock.getStock() - item.getQuantity());
        }
        for (CreateOrderRequest.Item item : request.getItems()) {
            cartItemRepository.deleteSelectedItem(customerId, item.getVariantId(), item.getColorId().toUpperCase());
        }
        if (orderPromotion != null) {
            long usedCount = orderRepository
                    .countByCustomer_UserIdAndOrderPromotion_PromotionId(
                            customerId, orderPromotion.getPromotionId());
            if (usedCount + 1 >= orderPromotion.getMaxOrderUsage()) {
                orderPromotion.setStatus(false);
            }
        }
        if (shippingPromotion != null) {
            long usedCount = orderRepository.countByCustomer_UserIdAndShippingPromotion_PromotionId(
                    customerId, shippingPromotion.getPromotionId());
            if (usedCount + 1 >= shippingPromotion.getMaxOrderUsage()) {
                shippingPromotion.setStatus(false);
            }
        }
        String paymentQrUrl = null;
        if (request.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            paymentQrUrl = generateVietQrCodeUrl(totalAmount, orderId);
        }
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
    public void updateOrderStatus(String orderId, OrderStatus newStatus, String trackingCode) {
        Order order = orderRepository.findById(orderId)
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
            order.setOrderStatus(OrderStatus.CONFIRMED);
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
            boolean hasActiveOrders = orderRepository.existsByCustomer_UserIdAndOrderStatusIn(customerId,
                    List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING));
            userService.lockIfNeeded(customerId, failedDeliveryCount, hasActiveOrders);
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
            boolean hasActiveOrders = orderRepository.existsByCustomer_UserIdAndOrderStatusIn(customerId,
                    List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.DELIVERED));
            userService.lockIfPendingAndNoActiveOrders(customerId, hasActiveOrders);
        }
        else {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Không thể chuyển trạng thái đơn hàng từ " + currentStatus + " sang " + newStatus);
        }
        orderRepository.save(order);
    }

    @Transactional
    public void cancelCustomerOrder(String customerId, String orderId, String reason) {
        reason = reason.trim();
        if (reason == null || reason.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lý do hủy đơn không được để trống");
        }
        if (reason.length() > 500) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lý do hủy đơn không được vượt quá 500 ký tự");
        }
        Order order = orderRepository
                .findByOrderIdAndCustomer_UserIdForUpdate(orderId, customerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ được hủy đơn hàng đang chờ xác nhận");
        }
        if (order.getPaymentStatus() == PaymentStatus.WAITING) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng đang chờ xác nhận thanh toán, không thể hủy");
        }
        for (OrderDetail detail : order.getOrderDetails()) {
            ColorStock stock = colorStockRepository
                    .findByVariantAndColorForUpdate(detail.getVariant().getVariantId(), detail.getColor().getColorId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin tồn kho"));
            stock.setStock(stock.getStock() + detail.getQuantity());
        }
        Cancellation cancellation = Cancellation.builder()
                .order(order)
                .reason(reason)
                .cancelledBy(order.getCustomer())
                .cancelledAt(LocalDateTime.now())
                .compensationPromotion(null)
                .build();
        cancellationRepository.save(cancellation);
        order.setOrderStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }
        order.setCancellation(cancellation);
        orderRepository.saveAndFlush(order);
        List<OrderStatus> activeStatuses = List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED,
                OrderStatus.SHIPPING, OrderStatus.DELIVERED);
        boolean hasActiveOrders = orderRepository
                .existsByCustomer_UserIdAndOrderStatusIn(customerId, activeStatuses);
        userService.lockIfPendingAndNoActiveOrders(customerId, hasActiveOrders);
    }

    @Transactional
    public void cancelStaffOrder(String staffId, String orderId, String reason) {
        reason = reason.trim();
        if (reason == null || reason.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lý do hủy đơn không được để trống");
        }
        if (reason.length() > 500) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lý do hủy đơn không được vượt quá 500 ký tự");
        }
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên"));
        Order order = orderRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ được hủy đơn hàng đang chờ xác nhận");
        }
        if (order.getPaymentStatus() == PaymentStatus.WAITING) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng đang chờ xác nhận thanh toán, không thể hủy");
        }
        for (OrderDetail detail : order.getOrderDetails()) {

            ColorStock stock = colorStockRepository
                    .findByVariantAndColorForUpdate(
                        detail.getVariant().getVariantId(), detail.getColor().getColorId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin tồn kho"));
            stock.setStock(stock.getStock() + detail.getQuantity());
        }
        Promotion compensationPromotion = promotionService.createCompensationPromotion(
                order.getCustomer(), order.getTotalAmount());
        Cancellation cancellation = Cancellation.builder()
                .order(order)
                .reason(reason)
                .cancelledBy(staff)
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
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.DELIVERED);
        boolean hasActiveOrders = orderRepository.existsByCustomer_UserIdAndOrderStatusIn(
                order.getCustomer().getUserId(), activeStatuses);
        userService.lockIfPendingAndNoActiveOrders(order.getCustomer().getUserId(), hasActiveOrders);
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

    private int calculateDiscount(Promotion promotion, int amount) {
        int discount;
        if (promotion.getDiscountType() == DiscountType.PERCENT) {
            discount = amount * promotion.getDiscountValue() / 100;
            if (promotion.getMaxDiscountValue() > 0) {
                discount = Math.min(discount, promotion.getMaxDiscountValue());
            }
        } else {
            discount = Math.min(promotion.getDiscountValue(), amount);
        }
        return discount;
    }

    private Promotion findBestProductPromotion(Product product, User customer) {
        LocalDate today = LocalDate.now();
        List<Promotion> productPromotions = promotionRepository
                .findAllByApplyTypeAndStatusIsTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    PromotionApplyType.PRODUCT, today, today);

        Promotion bestProductPromotion = null;
        int bestProductDiscount = 0;
        for (Promotion promotion : productPromotions) {
            if (promotion.getCustomer() != null
                    && !promotion.getCustomer().getUserId().equals(customer.getUserId())) {
                continue;
            }
            List<PromotionProduct> productRules = promotionProductRepository
                    .findByPromotion_PromotionId(promotion.getPromotionId());
            List<PromotionCategory> categoryRules = promotionCategoryRepository
                    .findByPromotion_PromotionId(promotion.getPromotionId());
            List<PromotionBrand> brandRules = promotionBrandRepository
                    .findByPromotion_PromotionId(promotion.getPromotionId());
            boolean productMatch = productRules.stream().anyMatch(rule ->
                    rule.getProduct().getProductId().equals(product.getProductId()));
            boolean categoryMatch = categoryRules.stream().anyMatch(rule ->
                            rule.getCategory().getCategoryId().equals(product.getCategory().getCategoryId()));
            boolean brandMatch = brandRules.stream().anyMatch(rule ->
                    rule.getId().getBrand().equals(product.getBrand()));
            boolean hasCategoryRule = !categoryRules.isEmpty();
            boolean hasBrandRule = !brandRules.isEmpty();
            boolean applicable = productMatch;
            if (hasBrandRule && hasCategoryRule) {
                applicable = applicable || (brandMatch && categoryMatch);
            } else {
                applicable = applicable || brandMatch || categoryMatch;
            }
            if (!applicable) {
                continue;
            }
            int discount = calculateDiscount(promotion, product.getSalePrice());
            if (discount > bestProductDiscount) {
                bestProductDiscount = discount;
                bestProductPromotion = promotion;
            }
        }
        return bestProductPromotion;
    }

    private Promotion validateOrderPromotion(String code, User customer, int productTotal) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Promotion promotion = promotionRepository
                .findByCodeAndApplyTypeAndStatusIsTrue(code.trim(), PromotionApplyType.ORDER)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Mã giảm giá đơn hàng không hợp lệ"));
        validatePromotion(promotion, customer, productTotal);
        if (orderRepository.existsByCustomer_UserIdAndOrderPromotion_PromotionId(
                customer.getUserId(), promotion.getPromotionId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Bạn đã sử dụng mã giảm giá này");
        }
        long usedCount = orderRepository.countByOrderPromotion_PromotionId(promotion.getPromotionId());
        if (promotion.getMaxOrderUsage() != null && usedCount >= promotion.getMaxOrderUsage()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết lượt sử dụng");
        }
        return promotion;
    }

    private Promotion validateShippingPromotion(String code, User customer, int productTotal) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Promotion promotion = promotionRepository
                .findByCodeAndApplyTypeAndStatusIsTrue(code.trim(), PromotionApplyType.SHIPPING)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Mã giảm phí vận chuyển không hợp lệ"));
        validatePromotion(promotion, customer, productTotal);
        if (orderRepository
                .existsByCustomer_UserIdAndShippingPromotion_PromotionId(customer.getUserId(), promotion.getPromotionId()
                )) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Bạn đã sử dụng mã giảm giá này");
        }

        long usedCount = orderRepository.countByShippingPromotion_PromotionId(promotion.getPromotionId());
        if (promotion.getMaxOrderUsage() != null && usedCount >= promotion.getMaxOrderUsage()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết lượt sử dụng");
        }
        return promotion;
    }

    private void validatePromotion(Promotion promotion, User customer, int productTotal) {
        LocalDate today = LocalDate.now();
        if (!promotion.isStatus()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã giảm giá hiện không hoạt động");
        }
        if (today.isBefore(promotion.getStartDate()) || today.isAfter(promotion.getEndDate())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn hoặc chưa có hiệu lực");
        }
        if (productTotal < promotion.getMinOrderValue()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa đạt giá trị tối thiểu để sử dụng mã");
        }
        if (promotion.getCustomer() != null && !promotion.getCustomer().getUserId().equals(customer.getUserId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã giảm giá không dành cho tài khoản của bạn");
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
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }
        ReturnRecord returnRecord = ReturnRecord.builder()
                .order(order)
                .returnType(ReturnType.FAILED_DELIVERY)
                .status(ReturnStatus.IN_TRANSIT)
                .note("Đơn hàng giao thất bại, hàng đang được hoàn về cửa hàng")
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
