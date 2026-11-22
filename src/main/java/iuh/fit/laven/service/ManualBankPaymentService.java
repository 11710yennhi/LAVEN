package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.order.ManualPaymentRequest;
import iuh.fit.laven.dto.response.order.StaffOrderPaymentResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.BankTransaction;
import iuh.fit.laven.model.Order;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import iuh.fit.laven.model.enums.UserRole;
import iuh.fit.laven.repository.BankTransactionRepository;
import iuh.fit.laven.repository.OrderRepository;
import iuh.fit.laven.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

/** Chỉ dùng khi webhook lỗi và nhân viên đã kiểm tra giao dịch tiền vào thực tế. */
@Service
@RequiredArgsConstructor
public class ManualBankPaymentService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BankTransactionRepository bankTransactionRepository;

    @Value("${payment.vietqr.account-number:}") private String storeAccountNumber;

    @Transactional
    public StaffOrderPaymentResponse confirm(String orderId, String staffId, ManualPaymentRequest request) {
        Order order = orderRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER
                || order.getOrderStatus() != OrderStatus.PENDING
                || (order.getPaymentStatus() != PaymentStatus.PENDING
                    && order.getPaymentStatus() != PaymentStatus.WAITING
                    && order.getPaymentStatus() != PaymentStatus.FAILED)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn không còn chờ xác nhận chuyển khoản");
        }
        if (!StringUtils.hasText(storeAccountNumber)
                || !storeAccountNumber.equals(request.getAccountNumber())
                || request.getAmountIn() == null
                || request.getAmountIn().longValue() != order.getTotalAmount().longValue()
                || request.getTransferContent() == null
                || !request.getTransferContent().toUpperCase(Locale.ROOT).contains(orderId.toUpperCase(Locale.ROOT))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tài khoản, số tiền hoặc nội dung không khớp đơn");
        }
        String transactionId = "MANUAL:" + request.getBankReference().trim();
        if (bankTransactionRepository.existsById(transactionId)) {
            throw new AppException(HttpStatus.CONFLICT, "Mã giao dịch đã dùng để xác nhận đơn khác");
        }
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Không tìm thấy nhân viên"));
        if (staff.getRole() != UserRole.STAFF && staff.getRole() != UserRole.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ nhân viên được xác nhận thủ công");
        }
        LocalDateTime now = LocalDateTime.now();
        bankTransactionRepository.save(new BankTransaction(transactionId, order, request.getAmountIn(),
                request.getAccountNumber(), request.getTransferContent(), "MANUAL_VERIFIED",
                request.getTransactionDate(), now));
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidAt(now);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(now);
        order.setConfirmedBy(staff);
        return StaffOrderPaymentResponse.builder()
                .orderId(orderId)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(now)
                .message("Đã xác nhận sau khi nhân viên kiểm tra sao kê")
                .build();
    }
}
