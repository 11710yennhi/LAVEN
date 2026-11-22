package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.order.ManualPaymentRequest;
import iuh.fit.laven.dto.response.order.StaffOrderPaymentResponse;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.ManualBankPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API ngoại lệ cho nhân viên; bình thường SePay webhook tự xác nhận đơn. */
@RestController
@RequestMapping("/api/staff/orders")
@RequiredArgsConstructor
public class ManualBankPaymentController {
    private final ManualBankPaymentService manualBankPaymentService;

    @PostMapping("/{orderId}/payment/manual")
    public StaffOrderPaymentResponse confirm(@PathVariable String orderId,
            @Valid @RequestBody ManualPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails staff) {
        return manualBankPaymentService.confirm(orderId, staff.getUserId(), request);
    }
}
