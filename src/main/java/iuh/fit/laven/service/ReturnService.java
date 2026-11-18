package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.order.ReturnRequest;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.*;
import iuh.fit.laven.model.enums.*;
import iuh.fit.laven.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnEvidenceRepository returnEvidenceRepository;
    private final ShipCostRepository shipCostRepository;

    @Transactional
    public void requestReturn(String orderId, String customerId, ReturnRequest request) {
        Order order = orderRepository.findOrderByOrderIdIsAndCustomer_UserId(orderId, customerId);

        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ được yêu cầu trả hàng với đơn hàng đã hoàn thành");
        }
        LocalDateTime deadline = order.getCompletedAt().plusDays(7);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đã quá thời hạn 7 ngày kể từ khi đơn hàng hoàn thành");
        }
        if (returnRecordRepository.existsByOrder_OrderId(orderId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng đã tồn tại yêu cầu trả hàng");
        }
        ReturnReason reason = request.getReturnReason();
        if (reason == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng chọn lý do trả hàng");
        }
        List<OrderDetail> orderDetails = orderDetailRepository.findAllById_OrderId(orderId);
        if (orderDetails.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng không có sản phẩm");
        }
        List<ReturnRequest.ReturnItemRequest> requestedItems = new ArrayList<>();
        if (reason == ReturnReason.NOT_RECEIVED) {
            for (OrderDetail detail : orderDetails) {
                ReturnRequest.ReturnItemRequest item = ReturnRequest.ReturnItemRequest.builder()
                        .variantId(detail.getVariant().getVariantId())
                        .colorId(detail.getColor().getColorId())
                        .quantity(detail.getQuantity())
                        .build();
                requestedItems.add(item);
            }

        } else {
            if (request.getNote() == null || request.getNote().isBlank()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng nhập ghi chú khi yêu cầu trả hàng");
            }
            if (request.getVideoUrl() == null || request.getVideoUrl().isBlank()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng cung cấp video kiểm hàng");
            }
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng chọn sản phẩm cần trả");
            }
            requestedItems = request.getItems();
        }
        Set<String> selectedProducts = new HashSet<>();
        int returnProductValue = 0;
        int totalReturnQuantity = 0;
        int totalPurchasedQuantity = 0;
        for (OrderDetail detail : orderDetails) {
            totalPurchasedQuantity += detail.getQuantity();
        }
        List<ReturnItem> returnItems = new ArrayList<>();
        for (ReturnRequest.ReturnItemRequest item : requestedItems) {
            String productKey = item.getVariantId() + "_" + item.getColorId();
            if (!selectedProducts.add(productKey)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm bị trùng trong danh sách trả hàng");
            }
            OrderDetail matchedDetail = orderDetails.stream()
                    .filter(detail ->
                            detail.getVariant().getVariantId().equals(item.getVariantId())
                                    && detail.getColor().getColorId().equals(item.getColorId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm không thuộc đơn hàng"));
            if (item.getQuantity() > matchedDetail.getQuantity()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Số lượng trả vượt quá số lượng đã mua");
            }
            totalReturnQuantity += item.getQuantity();
            int itemValue = matchedDetail.getUnitPrice() * item.getQuantity();
            returnProductValue += itemValue;
            ReturnItem returnItem = ReturnItem.builder()
                    .id(ReturnItemId.builder()
                            .orderId(order.getOrderId())
                            .variantId(item.getVariantId())
                            .colorId(item.getColorId())
                            .build())
                    .order(order)
                    .variant(matchedDetail.getVariant())
                    .color(matchedDetail.getColor())
                    .quantity(item.getQuantity())
                    .damagedQuantity(0)
                    .build();

            returnItems.add(returnItem);
        }
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            if (request.getRefundBankName() == null
                    || request.getRefundBankName().isBlank()
                    || request.getRefundAccountNumber() == null
                    || request.getRefundAccountNumber().isBlank()
                    || request.getRefundAccountName() == null
                    || request.getRefundAccountName().isBlank()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Đơn COD yêu cầu cung cấp đầy đủ thông tin ngân hàng");
            }
        }
        int returnShippingCost = 0;
        if (reason != ReturnReason.NOT_RECEIVED) {
            ShipCost shipCost = shipCostRepository
                    .findByCityIgnoreCase(order.getReceiverCity())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Không tìm thấy phí vận chuyển của thành phố"));
            returnShippingCost = shipCost.getPriceShip();
        }
        int refundAmount;
        int productTotal = order.getProductTotal();
        int orderDiscount = order.getOrderDiscount();
        int shippingFee = order.getShippingFee();
        int shippingDiscount = order.getShippingDiscount();
        if (reason == ReturnReason.NOT_RECEIVED) {
            refundAmount = order.getTotalAmount();
        } else {
            int allocatedDiscount = 0;
            if (productTotal > 0) {
                allocatedDiscount = (orderDiscount * returnProductValue / productTotal);
            }
            refundAmount = returnProductValue - allocatedDiscount;
            if (totalReturnQuantity == totalPurchasedQuantity) {
                refundAmount += shippingFee - shippingDiscount;
            }
        }
        ReturnRecord returnRecord = ReturnRecord.builder()
                .order(order)
                .returnType(ReturnType.CUSTOMER_RETURN)
                .returnReason(reason)
                .note(request.getNote())
                .status(ReturnStatus.REQUESTED)
                .returnShippingCost(returnShippingCost)
                .refundAmount(refundAmount)
                .refundBankName(request.getRefundBankName())
                .refundAccountNumber(request.getRefundAccountNumber())
                .refundAccountName(request.getRefundAccountName())
                .createdAt(LocalDateTime.now())
                .build();
        returnRecordRepository.save(returnRecord);
        returnItemRepository.saveAll(returnItems);
        List<ReturnEvidence> evidences = new ArrayList<>();
        if (request.getVideoUrl() != null && !request.getVideoUrl().isBlank()) {
            evidences.add(ReturnEvidence.builder()
                    .order(order)
                    .mediaUrl(request.getVideoUrl())
                    .build());
        }
        if (request.getImageUrls() != null) {
            for (String imageUrl : request.getImageUrls()) {
                if (imageUrl != null && !imageUrl.isBlank()) {
                    evidences.add(ReturnEvidence.builder()
                            .order(order)
                            .mediaUrl(imageUrl)
                            .build());
                }
            }
        }
        if (!evidences.isEmpty()) {
            returnEvidenceRepository.saveAll(evidences);
        }
    }
}
