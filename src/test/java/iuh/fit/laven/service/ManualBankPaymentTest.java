package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.order.ManualPaymentRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ManualBankPaymentTest {
    private static final String ORDER_ID = "ORD2026112212300000";
    private final OrderRepository orders = mock(OrderRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final BankTransactionRepository transactions = mock(BankTransactionRepository.class);
    private final ManualBankPaymentService service = new ManualBankPaymentService(orders, users, transactions);

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "storeAccountNumber", "5817102004");
    }

    @Test
    void staffConfirmationStoresBankRecordAndConfirmsOrder() {
        Order order = pendingOrder();
        User staff = mock(User.class);
        when(staff.getRole()).thenReturn(UserRole.STAFF);
        when(orders.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(users.findById("USR2026112200000001")).thenReturn(Optional.of(staff));

        service.confirm(ORDER_ID, "USR2026112200000001", validRequest());

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        assertSame(staff, order.getConfirmedBy());
        assertNotNull(order.getPaidAt());
        verify(transactions).save(argThat(tx -> tx.getTransactionId().equals("MANUAL:FT123")
                && tx.getOrder() == order));
    }

    @Test
    void wrongAmountDoesNotConfirm() {
        Order order = pendingOrder();
        when(orders.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        ManualPaymentRequest request = validRequest();
        request.setAmountIn(1000L);

        assertThrows(AppException.class,
                () -> service.confirm(ORDER_ID, "USR2026112200000001", request));
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        verify(transactions, never()).save(any(BankTransaction.class));
    }

    @Test
    void usedReferenceDoesNotConfirmAnotherOrder() {
        Order order = pendingOrder();
        when(orders.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(transactions.existsById("MANUAL:FT123")).thenReturn(true);

        assertThrows(AppException.class,
                () -> service.confirm(ORDER_ID, "USR2026112200000001", validRequest()));
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        verifyNoInteractions(users);
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setOrderId(ORDER_ID);
        order.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(2000);
        return order;
    }

    private ManualPaymentRequest validRequest() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setBankReference("FT123");
        request.setAccountNumber("5817102004");
        request.setAmountIn(2000L);
        request.setTransferContent("CHUYEN TIEN " + ORDER_ID);
        request.setTransactionDate(LocalDateTime.of(2026, 11, 22, 12, 30));
        return request;
    }
}
