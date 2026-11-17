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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.getCustomerOrders(me.getUserId(), orderStatus, page, size);
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

    @PostMapping
    public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request,
                                            @AuthenticationPrincipal CustomUserDetails me) {
        return orderService.createOrder(me.getUserId(), request);
    }


    //STAFF

    @RestController
    @RequestMapping("/api/staff/orders")
    @RequiredArgsConstructor
    public class StaffOrderController {
        private final OrderService orderService;
        @GetMapping
        public Page<StaffOrderSummaryResponse> getStaffOrders(
                @ModelAttribute StaffOrderFilterRequest filter) {
            return orderService.getStaffOrders(filter);
        }
    }

    @GetMapping("/orders/{id}")
    public StaffOrderDetailResponse getStaffOrderDetail(@PathVariable String id) {
        return orderService.getStaffOrderDetail(id);
    }

    @PutMapping("/staff/orders/{id}/payment")
    public StaffOrderPaymentResponse updateOrderPayment(@PathVariable String id,
                                                        @RequestParam(required = false) PaymentStatus paymentStatus){
        return orderService.updateOrderPayment(id, paymentStatus);
    }


}
