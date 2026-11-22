package iuh.fit.laven.controller;

import org.springframework.http.MediaType;
import iuh.fit.laven.dto.request.SepayWebhookRequest;
import iuh.fit.laven.dto.response.WebhookAckResponse;
import iuh.fit.laven.service.BankPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** SePay gọi API này qua URL công khai của LAVEN, không phải React gọi. */
@RestController
@RequestMapping(value = "/api/payments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PaymentWebhookController {
    private final BankPaymentService bankPaymentService;

    @PostMapping("/webhook")
    public WebhookAckResponse receive(@RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String authorization,
                                      @RequestBody SepayWebhookRequest transaction) {
        return new WebhookAckResponse(true, bankPaymentService.processWebhook(authorization, transaction));
    }
}
