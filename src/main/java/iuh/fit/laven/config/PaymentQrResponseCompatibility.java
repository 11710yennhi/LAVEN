package iuh.fit.laven.config;

import iuh.fit.laven.dto.response.order.PaymentQrResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Giữ JSON QR của nhóm Hà và thêm tên field mà React hiện đang đọc.
 * Không thay endpoint hoặc lớp PaymentQrResponse của nhóm Hà.
 */
@ControllerAdvice
public class PaymentQrResponseCompatibility implements ResponseBodyAdvice<Object> {
    @Value("${payment.vietqr.bank-id:}")
    private String bankId;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return PaymentQrResponse.class.isAssignableFrom(returnType.getParameterType())
                && MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof PaymentQrResponse qr)) {
            return body;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        // Field gốc cho các client đã dùng API của Hà.
        result.put("orderId", qr.getOrderId());
        result.put("bankName", qr.getBankName());
        result.put("bankAccount", qr.getBankAccount());
        result.put("accountName", qr.getAccountName());
        result.put("amount", qr.getAmount());
        result.put("transferContent", qr.getTransferContent());
        result.put("qrCodeUrl", qr.getQrCodeUrl());
        result.put("paymentStatus", qr.getPaymentStatus());
        // Tên mà PaymentPage.jsx đang dùng.
        result.put("bankId", bankId);
        result.put("accountNo", qr.getBankAccount());
        result.put("content", qr.getTransferContent());
        result.put("qrUrl", qr.getQrCodeUrl());
        return result;
    }
}
