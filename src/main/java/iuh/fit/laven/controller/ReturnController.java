package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.order.ReceiveReturnRequest;
import iuh.fit.laven.dto.request.order.ReturnRequest;
import iuh.fit.laven.dto.response.order.ReturnDetailResponse;
import iuh.fit.laven.model.enums.ReturnStatus;
import iuh.fit.laven.model.enums.ReturnType;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReturnController {


    private final ReturnService returnService;

    // ===== CUSTOMER =====

    @PostMapping("/customer/orders/{id}/return")
    public void requestReturn(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails me,
            @ModelAttribute ReturnRequest request
    ) {
        returnService.requestReturn(id, me.getUserId(), request);
    }

    /** Khách xem yêu cầu trả hàng */
    @GetMapping("/customer/orders/{id}/return")
    public ReturnDetailResponse getCustomerReturn(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        return returnService.getCustomerReturn(id, me.getUserId());
    }

    // ===== STAFF / ADMIN =====

    /** Danh sách phiếu trả hàng / giao thất bại, mới nhất trước */
    @GetMapping("/staff/returns")
    public Page<ReturnDetailResponse> getStaffReturns(
            @RequestParam(required = false) ReturnStatus status,
            @RequestParam(required = false) ReturnType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return returnService.getStaffReturns(status, type, page, size);
    }

    /** Chi tiết phiếu trả hàng của đơn */
    @GetMapping("/staff/returns/{orderId}")
    public ReturnDetailResponse getStaffReturn(@PathVariable String orderId) {
        return returnService.getStaffReturn(orderId);
    }

    /** Nhân viên duyệt yêu cầu */
    @PutMapping("/staff/returns/{orderId}/approve")
    public ReturnDetailResponse approveReturn(@PathVariable String orderId) {
        return returnService.approveReturn(orderId);
    }

    /** Nhân viên từ chối yêu cầu */
    @PutMapping("/staff/returns/{orderId}/reject")
    public ReturnDetailResponse rejectReturn(
            @PathVariable String orderId,
            @RequestParam(required = false) String reason
    ) {
        return returnService.rejectReturn(orderId, reason);
    }

    /** Cập nhật hàng đang vận chuyển về shop */
    @PutMapping("/staff/returns/{orderId}/in-transit")
    public ReturnDetailResponse markReturnInTransit(@PathVariable String orderId) {
        return returnService.markReturnInTransit(orderId);
    }

    /** Nhận và kiểm tra hàng trả (cả trả hàng và giao thất bại) */
    @PutMapping("/staff/returns/{orderId}/receive")
    public ReturnDetailResponse receiveReturn(
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestBody(required = false) ReceiveReturnRequest request
    ) {
        return returnService.receiveReturn(orderId, me.getUserId(), request);
    }
}
