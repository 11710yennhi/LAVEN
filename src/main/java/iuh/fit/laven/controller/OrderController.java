package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.order.CreateOrderRequest;
import iuh.fit.laven.dto.request.order.OrderPreviewRequest;
import iuh.fit.laven.dto.request.order.StaffOrderFilterRequest;
import iuh.fit.laven.dto.response.OrderPreviewResponse;
import iuh.fit.laven.dto.response.order.*;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentStatus;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    //CUSTOMER

    @GetMapping("/customer/orders")
    public Page<OrderSummaryResponse> getCustomerOrders(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.getCustomerOrders(me.getUserId(), orderStatus, sortDirection, page, size);
    }

    @GetMapping("/customer/orders/{id}")
    public CustomerOrderDetailResponse getCustomerOrderDetail(
            @PathVariable String id, @AuthenticationPrincipal CustomUserDetails me) {
        return orderService.getCustomerOrderDetail(id, me.getUserId());
    }

    @GetMapping("/customer/orders/{id}/payment-qr")
    public PaymentQrResponse getCustomerPaymentQr(@PathVariable String id,
                                                  @AuthenticationPrincipal CustomUserDetails me) {
        return orderService.getCustomerPaymentQr(id, me.getUserId());
    }

    @PostMapping("/customer/orders/{id}/payment-notify")
    public PaymentNotifyResponse notifyCustomerPayment(@PathVariable String id,
                                                           @AuthenticationPrincipal CustomUserDetails me) {
        return orderService.notifyCustomerPayment(id, me.getUserId());
    }

    @PostMapping("/customer/orders/preview")
    public OrderPreviewResponse previewOrder(@Valid @RequestBody OrderPreviewRequest request,
                                            @AuthenticationPrincipal CustomUserDetails me) {
        return orderService.previewOrder(me.getUserId(), request);
    }

    @PostMapping("/customer/orders")
    public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request,
                                            @AuthenticationPrincipal CustomUserDetails me) {
        return orderService.createOrder(me.getUserId(), request);
    }

    @PostMapping("/customer/orders/{id}/cancel")
    public void cancelCustomerOrder(@PathVariable String id, @RequestParam String reason,
            @AuthenticationPrincipal CustomUserDetails me) {
        orderService.cancelCustomerOrder(me.getUserId(), id, reason);
    }

    //STAFF

    @GetMapping("/staff/orders")
    public Page<StaffOrderSummaryResponse> getStaffOrders(@ModelAttribute StaffOrderFilterRequest filter) {
        return orderService.getStaffOrders(filter);
    }

    @GetMapping("/staff/orders/{id}")
    public StaffOrderDetailResponse getStaffOrderDetail(@PathVariable String id) {
        return orderService.getStaffOrderDetail(id);
    }

    @PutMapping("/staff/orders/{id}/payment")
    public StaffOrderPaymentResponse updateOrderPayment(@PathVariable String id,
                                                        @RequestParam(required = false) PaymentStatus paymentStatus){
        return orderService.updateOrderPayment(id, paymentStatus);
    }

    @PutMapping("/staff/orders/{id}/status")
    public void updateOrderStatus(@PathVariable String id, @RequestParam OrderStatus status,
            @RequestParam(required = false) String trackingCode,
            @AuthenticationPrincipal CustomUserDetails me) {
        orderService.updateOrderStatus(id, status, trackingCode, me.getUserId());
    }

    /** Hủy nhiều đơn: ?orderIds=A&orderIds=B&reason=..., trả về { mã đơn: mã bồi thường } */
    @PutMapping("/staff/orders/bulk-cancel")
    public Map<String, String> cancelStaffOrders(@RequestParam List<String> orderIds, @RequestParam String reason,
            @AuthenticationPrincipal CustomUserDetails me) {
        return orderService.cancelStaffOrders(me.getUserId(), orderIds, reason);
    }

    @PutMapping("/staff/orders/{id}/cancel")
    public void cancelStaffOrder(@PathVariable String id, @RequestParam String reason,
            @AuthenticationPrincipal CustomUserDetails me){
        orderService.cancelStaffOrder(me.getUserId(), id, reason);
    }

}
