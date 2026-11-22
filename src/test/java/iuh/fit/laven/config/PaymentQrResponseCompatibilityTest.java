package iuh.fit.laven.config;

import iuh.fit.laven.dto.response.order.PaymentQrResponse;
import iuh.fit.laven.model.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaymentQrResponseCompatibilityTest {
    private PaymentQrResponse endpoint() { return null; }

    @Test
    void keepsOriginalFieldsAndAddsFieldsUsedByReact() throws Exception {
        PaymentQrResponseCompatibility advice = new PaymentQrResponseCompatibility();
        ReflectionTestUtils.setField(advice, "bankId", "MB");
        Method method = getClass().getDeclaredMethod("endpoint");
        MethodParameter returnType = new MethodParameter(method, -1);
        assertTrue(advice.supports(returnType, MappingJackson2HttpMessageConverter.class));

        PaymentQrResponse qr = PaymentQrResponse.builder()
                .orderId("ORD2026112212300000")
                .bankName("MB Bank")
                .bankAccount("5817102004")
                .accountName("NGUYEN THI YEN NHI")
                .amount(2000)
                .transferContent("ORD2026112212300000")
                .qrCodeUrl("https://img.vietqr.io/image/MB-5817102004-compact2.png")
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        Object body = advice.beforeBodyWrite(qr, returnType, null,
                MappingJackson2HttpMessageConverter.class, null, null);

        assertInstanceOf(Map.class, body);
        Map<?, ?> json = (Map<?, ?>) body;
        assertEquals("MB", json.get("bankId"));
        assertEquals(json.get("bankAccount"), json.get("accountNo"));
        assertEquals(json.get("transferContent"), json.get("content"));
        assertEquals(json.get("qrCodeUrl"), json.get("qrUrl"));
        assertEquals(PaymentStatus.PENDING, json.get("paymentStatus"));
    }
}
