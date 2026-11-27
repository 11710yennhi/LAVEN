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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BankPaymentServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final BankTransactionRepository transactions = mock(BankTransactionRepository.class);
    private final BankPaymentService service = new BankPaymentService(orders, transactions);
    private final String orderId = "ORD2026112212300000";

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "sepayApiKey", "secret");
        ReflectionTestUtils.setField(service, "accountNumber", "5817102004");
    }

    @Test
    void validIncomingTransferMarksPaidAndLeavesOrderPending() {
        Order order = pendingOrder();
        when(orders.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(order));

        String message = service.processWebhook("Apikey secret", incomingTransfer());

        assertTrue(message.contains(orderId));
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        assertNotNull(order.getPaidAt());
        assertNull(order.getConfirmedAt());
        assertNull(order.getConfirmedBy());
        verify(transactions).save(any(BankTransaction.class));
    }

    @Test
    void wrongAccountOrAmountDoesNotMarkPaid() {
        SepayWebhookRequest transfer = incomingTransfer();
        transfer.setAccountNumber("other-account");
        service.processWebhook("Apikey secret", transfer);
        verifyNoInteractions(orders, transactions);

        transfer.setAccountNumber("5817102004");
        transfer.setTransferAmount(1900L);
        Order order = pendingOrder();
        when(orders.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(order));
        service.processWebhook("Apikey secret", transfer);
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        verify(transactions, never()).save(any());
    }

    @Test
    void wrongApiKeyIsRejectedBeforeReadingOrder() {
        assertThrows(AppException.class, () -> service.processWebhook("Apikey wrong", incomingTransfer()));
        verifyNoInteractions(orders, transactions);
    }

    @Test
    void repeatedSepayTransactionDoesNotConfirmAgain() {
        when(transactions.existsById("100368418")).thenReturn(true);
        assertEquals("Giao dịch đã xử lý", service.processWebhook("Apikey secret", incomingTransfer()));
        verifyNoInteractions(orders);
        verify(transactions, never()).save(any());
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setTotalAmount(2000);
        return order;
    }

    private SepayWebhookRequest incomingTransfer() {
        SepayWebhookRequest transfer = new SepayWebhookRequest();
        transfer.setId("100368418");
        transfer.setTransferType("in");
        transfer.setAccountNumber("5817102004");
        transfer.setTransferAmount(2000L);
        transfer.setContent("CHUYEN TIEN " + orderId);
        transfer.setTransactionDate("2026-11-22 12:30:00");
        transfer.setGateway("MBBank");
        return transfer;
    }
}
