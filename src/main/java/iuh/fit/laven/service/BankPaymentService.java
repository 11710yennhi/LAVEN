package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.SepayWebhookRequest;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.BankTransaction;
import iuh.fit.laven.model.Order;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import iuh.fit.laven.repository.BankTransactionRepository;
import iuh.fit.laven.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Đối chiếu giao dịch SePay với đơn chuyển khoản và xác nhận thanh toán. */
@Service
@RequiredArgsConstructor
public class BankPaymentService {
    private static final Pattern ORDER_ID = Pattern.compile("(?i)ORD\\d{16}");
    private static final DateTimeFormatter SEPAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderRepository orderRepository;
    private final BankTransactionRepository bankTransactionRepository;

    @Value("${payment.sepay-api-key:}") private String sepayApiKey;
    @Value("${payment.vietqr.account-number:}") private String accountNumber;

    /** Chỉ HTTP request có API key SePay đúng mới được đi vào phần đối chiếu. */
    @Transactional
    public String processWebhook(String authorization, SepayWebhookRequest transaction) {
        verifyApiKey(authorization);
        if (transaction == null || !"in".equalsIgnoreCase(transaction.getTransferType())) {
            return "Bỏ qua giao dịch không phải tiền vào";
        }
        if (!StringUtils.hasText(accountNumber) || !accountNumber.equals(transaction.getAccountNumber())) {
            return "Bỏ qua giao dịch khác tài khoản nhận";
        }
        String transactionId = transaction.getId();
        if (!StringUtils.hasText(transactionId) || transactionId.length() > 100
                || transaction.getTransferAmount() == null || transaction.getTransferAmount() <= 0) {
            return "Bỏ qua giao dịch thiếu ID hoặc số tiền";
        }
        if (bankTransactionRepository.existsById(transactionId)) {
            return "Giao dịch đã xử lý";
        }

        // SePay có thể đặt mã đơn ở code hoặc trong nội dung chuyển khoản.
        String content = safe(transaction.getCode()) + " " + safe(transaction.getContent())
                + " " + safe(transaction.getDescription());
        Matcher matcher = ORDER_ID.matcher(content);
        if (!matcher.find()) {
            return "Không tìm thấy mã đơn trong nội dung chuyển khoản";
        }
        String orderId = matcher.group().toUpperCase();

        // Khóa dòng đơn: hai webhook đến cùng lúc không thể cùng xác nhận một đơn.
        Order order = orderRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (order == null || order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER
                || order.getOrderStatus() != OrderStatus.PENDING
                || (order.getPaymentStatus() != PaymentStatus.PENDING
                    && order.getPaymentStatus() != PaymentStatus.WAITING)
                || transaction.getTransferAmount().longValue() != order.getTotalAmount().longValue()) {
            return "Giao dịch không khớp đơn đang chờ thanh toán";
        }

        LocalDateTime transactionDate;
        try {
            transactionDate = LocalDateTime.parse(transaction.getTransactionDate(), SEPAY_DATE);
        } catch (DateTimeParseException | NullPointerException ex) {
            return "Ngày giao dịch SePay không hợp lệ";
        }
        LocalDateTime now = LocalDateTime.now();
        // Lưu giao dịch và xác nhận thanh toán trong cùng transaction của MariaDB.
        bankTransactionRepository.save(new BankTransaction(transactionId, order,
                transaction.getTransferAmount(), accountNumber,
                content.strip().substring(0, Math.min(content.strip().length(), 1000)),
                transaction.getGateway(), transactionDate, now));
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidAt(now);
        return "Đã xác nhận thanh toán đơn " + orderId;
    }

    private void verifyApiKey(String authorization) {
        if (!StringUtils.hasText(sepayApiKey) || !MessageDigest.isEqual(
                ("Apikey " + sepayApiKey).getBytes(StandardCharsets.UTF_8),
                safe(authorization).getBytes(StandardCharsets.UTF_8))) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "API Key webhook không hợp lệ");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
