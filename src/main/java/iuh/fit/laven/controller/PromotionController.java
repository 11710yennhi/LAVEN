package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.CompensationTemplateRequest;
import iuh.fit.laven.dto.request.PromotionRequest;
import iuh.fit.laven.dto.response.PromotionResponse;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // ---- Staff: khuyến mãi công khai (UC29) ----
    @GetMapping("/staff/promotions")
    public ResponseEntity<List<PromotionResponse>> listPublic() {
        return ResponseEntity.ok(promotionService.listPublic());
    }

    @GetMapping("/staff/promotions/{id}")
    public ResponseEntity<PromotionResponse> getOne(@PathVariable("id") String id) {
        return ResponseEntity.ok(promotionService.getById(id));
    }

    @PostMapping("/staff/promotions")
    public ResponseEntity<PromotionResponse> create(@RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.createPublic(request));
    }

    @PutMapping("/staff/promotions/{id}")
    public ResponseEntity<PromotionResponse> update(@PathVariable("id") String id,
                                                    @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionService.updatePublic(id, request));
    }

    // ---- Customer: mã của tôi (UC14) ----
    @GetMapping("/customer/promotions")
    public ResponseEntity<List<PromotionResponse>> myPromotions(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(promotionService.listForCustomer(principal.getUserId()));
    }

    // ---- Admin: mã mẫu và mã bồi thường (UC33) ----
    @GetMapping("/admin/compensation-template")
    public ResponseEntity<PromotionResponse> getTemplate() {
        return ResponseEntity.ok(promotionService.getTemplate());
    }

    @PutMapping("/admin/compensation-template")
    public ResponseEntity<PromotionResponse> updateTemplate(@RequestBody CompensationTemplateRequest request) {
        return ResponseEntity.ok(promotionService.updateTemplate(request));
    }

    @GetMapping("/admin/compensation-promotions")
    public ResponseEntity<List<PromotionResponse>> listCompensation() {
        return ResponseEntity.ok(promotionService.listCompensation());
    }

    @PutMapping("/admin/compensation-promotions/{id}")
    public ResponseEntity<PromotionResponse> updateCompensation(@PathVariable("id") String id,
                                                                @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionService.updateCompensation(id, request));
    }
}
