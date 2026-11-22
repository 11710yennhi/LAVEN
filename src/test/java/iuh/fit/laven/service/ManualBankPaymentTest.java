package iuh.fit.laven.service;

import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.Order;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import iuh.fit.laven.repository.OrderRepository;
import iuh.fit.laven.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualBankPaymentTest {
    @Mock private OrderRepository orders;
    @Mock private UserRepository users;
    @InjectMocks private OrderService service;

    @Test
    void staffConfirmationRecordsWhoCheckedBankAndConfirmsOrder() {
        Order order = pendingOrder();
        User staff = mock(User.class);
        when(orders.findByOrderIdForUpdate("ORD2026112212300000")).thenReturn(Optional.of(order));
        when(users.findById("USR2026112200000001")).thenReturn(Optional.of(staff));

        service.confirmBankTransferManually("ORD2026112212300000", "USR2026112200000001", "FT123");

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        assertEquals("FT123", order.getManualPaymentReference());
        assertSame(staff, order.getConfirmedBy());
        assertNotNull(order.getPaidAt());
    }

    @Test
    void paidOrderCannotBeConfirmedTwice() {
        Order order = pendingOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        when(orders.findByOrderIdForUpdate("ORD2026112212300000")).thenReturn(Optional.of(order));

        assertThrows(AppException.class, () -> service.confirmBankTransferManually(
                "ORD2026112212300000", "USR2026112200000001", "FT123"));
        verifyNoInteractions(users);
    }

    @Test
    void bankReferenceCannotConfirmTwoOrders() {
        Order order = pendingOrder();
        when(orders.findByOrderIdForUpdate("ORD2026112212300000")).thenReturn(Optional.of(order));
        when(orders.existsByManualPaymentReference("FT123")).thenReturn(true);

        assertThrows(AppException.class, () -> service.confirmBankTransferManually(
                "ORD2026112212300000", "USR2026112200000001", "FT123"));
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        verifyNoInteractions(users);
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setOrderId("ORD2026112212300000");
        order.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        return order;
    }
}
