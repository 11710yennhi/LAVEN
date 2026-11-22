package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.CartItemRequest;
import iuh.fit.laven.dto.request.CartItemsRequest;
import iuh.fit.laven.dto.response.CartResponse;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** Guest: gửi giỏ localStorage lên để nhận giá hiện tại, tồn kho, cảnh báo. */
    @PostMapping("/public/cart/preview")
    public ResponseEntity<CartResponse> preview(@RequestBody CartItemsRequest request) {
        return ResponseEntity.ok(cartService.preview(request.getItems()));
    }

    @GetMapping("/customer/cart/items")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getUserId()));
    }

    @PostMapping("/customer/cart/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal CustomUserDetails principal,
                                                @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(principal.getUserId(), request));
    }

    @PutMapping("/customer/cart/items")
    public ResponseEntity<CartResponse> updateItem(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(principal.getUserId(), request));
    }

    @DeleteMapping("/customer/cart/items")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @RequestParam("variantId") String variantId,
                                                   @RequestParam("colorId") String colorId) {
        return ResponseEntity.ok(cartService.removeItem(principal.getUserId(), variantId, colorId));
    }

    /** Guest đăng nhập: gộp giỏ localStorage vào giỏ DB; client xóa localStorage sau khi thành công. */
    @PostMapping("/customer/cart/merge")
    public ResponseEntity<CartResponse> merge(@AuthenticationPrincipal CustomUserDetails principal,
                                              @RequestBody CartItemsRequest request) {
        return ResponseEntity.ok(cartService.merge(principal.getUserId(), request.getItems()));
    }
}
