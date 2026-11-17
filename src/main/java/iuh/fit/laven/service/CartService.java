package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.CartItemRequest;
import iuh.fit.laven.dto.response.CartItemResponse;
import iuh.fit.laven.dto.response.CartResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.Cart;
import iuh.fit.laven.model.CartItem;
import iuh.fit.laven.model.CartItemId;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.ColorStockId;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.User;
import iuh.fit.laven.repository.CartItemRepository;
import iuh.fit.laven.repository.CartRepository;
import iuh.fit.laven.repository.ColorStockRepository;
import iuh.fit.laven.repository.UserRepository;
import iuh.fit.laven.service.PromotionService.ScopedPromotion;
import iuh.fit.laven.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int DEFAULT_MAX_QUANTITY = 5;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ColorStockRepository colorStockRepository;
    private final UserRepository userRepository;
    private final PromotionService promotionService;

    // =====================================================================
    // UC08: giỏ hàng của Customer (lưu DB)
    // =====================================================================

    /** Xem giỏ. Món vượt mức cho phép (do tồn kho hoặc giới hạn đổi) được cắt về mức mới và cảnh báo. */
    @Transactional
    public CartResponse getCart(String customerId) {
        return buildCart(getOrCreateCart(customerId));
    }

    /** Thêm món: món đã có thì cộng dồn. */
    @Transactional
    public CartResponse addItem(String customerId, CartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);
        int quantity = requireQuantity(request.getQuantity());
        ColorStock stock = requireColorStock(request.getVariantId(), request.getColorId());
        checkSellable(stock);

        int existing = cartItemRepository.findById(itemId(cart, stock))
                .map(CartItem::getQuantity).orElse(0);
        int newQuantity = existing + quantity;
        validateQuantity(newQuantity, stock);
        upsert(cart, stock, newQuantity);
        return buildCart(cart);
    }

    /** Sửa số lượng: đặt đúng bằng số gửi lên. */
    @Transactional
    public CartResponse updateItem(String customerId, CartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);
        int quantity = requireQuantity(request.getQuantity());
        ColorStock stock = requireColorStock(request.getVariantId(), request.getColorId());
        checkSellable(stock);

        CartItem item = cartItemRepository.findById(itemId(cart, stock))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Món này không có trong giỏ hàng"));
        validateQuantity(quantity, stock);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return buildCart(cart);
    }

    @Transactional
    public CartResponse removeItem(String customerId, String variantId, String colorId) {
        Cart cart = getOrCreateCart(customerId);
        if (variantId == null || colorId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Thiếu mã biến thể hoặc mã màu");
        }
        CartItemId id = new CartItemId(cart.getCartId(), variantId.trim(), colorId.trim());
        cartItemRepository.findById(id).ifPresent(cartItemRepository::delete);
        return buildCart(cart);
    }

    // =====================================================================
    // Gộp giỏ localStorage vào giỏ DB khi Guest đăng nhập (quy trình 6 bước 5)
    // Trùng món thì cộng số lượng nhưng cắt ở min(stock, maxQuantityPerOrder); bỏ món ngừng bán.
    // =====================================================================
    @Transactional
    public CartResponse merge(String customerId, List<CartItemRequest> guestItems) {
        Cart cart = getOrCreateCart(customerId);

        // Gộp các dòng trùng trong chính danh sách gửi lên
        Map<String, Integer> merged = new LinkedHashMap<>();
        Map<String, CartItemRequest> keys = new LinkedHashMap<>();
        if (guestItems != null) {
            for (CartItemRequest g : guestItems) {
                if (g == null || g.getVariantId() == null || g.getColorId() == null
                        || g.getQuantity() == null || g.getQuantity() <= 0) {
                    continue;
                }
                String key = g.getVariantId().trim() + "|" + g.getColorId().trim();
                merged.merge(key, g.getQuantity(), Integer::sum);
                keys.putIfAbsent(key, g);
            }
        }

        for (Map.Entry<String, Integer> entry : merged.entrySet()) {
            CartItemRequest g = keys.get(entry.getKey());
            ColorStock stock = colorStockRepository
                    .findById(new ColorStockId(g.getVariantId().trim(), g.getColorId().trim()))
                    .orElse(null);
            if (stock == null || !isSellable(stock)) {
                continue; // món không tồn tại hoặc ngừng bán thì bỏ
            }
            int cap = Math.min(currentStock(stock), maxQuantity(stock.getVariant().getProduct()));
            if (cap <= 0) {
                continue;
            }
            int existing = cartItemRepository.findById(itemId(cart, stock))
                    .map(CartItem::getQuantity).orElse(0);
            upsert(cart, stock, Math.min(existing + entry.getValue(), cap));
        }
        return buildCart(cart);
    }

    // =====================================================================
    // UC03: Guest xem giỏ (client gửi danh sách lên, server trả giá, tồn kho, cảnh báo; không ghi DB)
    // =====================================================================
    @Transactional(readOnly = true)
    public CartResponse preview(List<CartItemRequest> items) {
        List<ScopedPromotion> promotions = promotionService.loadActiveProductPromotions();
        List<CartItemResponse> responses = new ArrayList<>();
        if (items != null) {
            for (CartItemRequest r : items) {
                if (r == null || r.getVariantId() == null || r.getColorId() == null) {
                    continue;
                }
                int quantity = r.getQuantity() == null ? 0 : r.getQuantity();
                ColorStock stock = colorStockRepository
                        .findById(new ColorStockId(r.getVariantId().trim(), r.getColorId().trim()))
                        .orElse(null);
                responses.add(buildItem(r.getVariantId().trim(), r.getColorId().trim(), quantity, stock, promotions));
            }
        }
        return toCartResponse(responses);
    }

    // =====================================================================
    // Nội bộ
    // =====================================================================
    private Cart getOrCreateCart(String customerId) {
        return cartRepository.findByCustomer_UserId(customerId).orElseGet(() -> {
            User customer = userRepository.findById(customerId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
            return cartRepository.save(Cart.builder()
                    .cartId(IdGenerator.nextUnique("CRT", cartRepository::existsById))
                    .customer(customer)
                    .build());
        });
    }

    private CartResponse buildCart(Cart cart) {
        List<ScopedPromotion> promotions = promotionService.loadActiveProductPromotions();
        List<CartItemResponse> responses = new ArrayList<>();
        for (CartItem item : cartItemRepository.findByCart_CartId(cart.getCartId())) {
            String variantId = item.getId().getVariantId();
            String colorId = item.getId().getColorId();
            ColorStock stock = colorStockRepository.findById(new ColorStockId(variantId, colorId)).orElse(null);
            CartItemResponse response = buildItem(variantId, colorId, item.getQuantity(), stock, promotions);
            // Số lượng bị cắt về mức cho phép thì ghi lại vào giỏ
            if (response.getQuantity() > 0 && response.getQuantity() != item.getQuantity()) {
                item.setQuantity(response.getQuantity());
                cartItemRepository.save(item);
            }
            responses.add(response);
        }
        return toCartResponse(responses);
    }

    private CartItemResponse buildItem(String variantId, String colorId, int quantity,
                                       ColorStock stock, List<ScopedPromotion> promotions) {
        if (stock == null) {
            return CartItemResponse.builder()
                    .variantId(variantId).colorId(colorId).quantity(quantity)
                    .available(false).warning("Sản phẩm không có màu này")
                    .build();
        }
        Product product = stock.getVariant().getProduct();
        int salePrice = product.getSalePrice();
        int discount = promotionService.findBestProductPromotion(product, promotions)
                .map(PromotionService.AppliedPromotion::discount).orElse(0);
        int unitPrice = salePrice - discount;
        int inStock = currentStock(stock);
        int max = maxQuantity(product);

        boolean sellable = isSellable(stock);
        boolean available = sellable && inStock > 0;
        String warning = null;
        int effectiveQuantity = quantity;
        if (!sellable) {
            warning = "Sản phẩm đã ngừng bán";
        } else if (inStock <= 0) {
            warning = "Hết hàng";
        } else {
            int cap = Math.min(inStock, max);
            if (quantity > cap) {
                effectiveQuantity = cap;
                warning = "Số lượng đã được điều chỉnh theo tồn kho hoặc giới hạn mỗi đơn";
            }
        }

        return CartItemResponse.builder()
                .variantId(variantId)
                .colorId(colorId)
                .colorName(stock.getColor().getName())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .size(stock.getVariant().getSize())
                .quantity(effectiveQuantity)
                .originalPrice(salePrice)
                .unitPrice(unitPrice)
                .subtotal(available ? effectiveQuantity * unitPrice : 0)
                .stock(inStock)
                .maxQuantityPerOrder(max)
                .available(available)
                .warning(warning)
                .build();
    }

    private CartResponse toCartResponse(List<CartItemResponse> items) {
        int total = items.stream().filter(CartItemResponse::isAvailable).mapToInt(CartItemResponse::getSubtotal).sum();
        return CartResponse.builder().items(items).estimatedTotal(total).build();
    }

    private void upsert(Cart cart, ColorStock stock, int quantity) {
        CartItemId id = itemId(cart, stock);
        CartItem item = cartItemRepository.findById(id).orElse(null);
        if (item == null) {
            item = CartItem.builder()
                    .id(id)
                    .cart(cart)
                    .variant(stock.getVariant())
                    .color(stock.getColor())
                    .quantity(quantity)
                    .build();
        } else {
            item.setQuantity(quantity);
        }
        cartItemRepository.save(item);
    }

    private CartItemId itemId(Cart cart, ColorStock stock) {
        return new CartItemId(cart.getCartId(), stock.getVariant().getVariantId(), stock.getColor().getColorId());
    }

    private ColorStock requireColorStock(String variantId, String colorId) {
        if (variantId == null || variantId.isBlank() || colorId == null || colorId.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Thiếu mã biến thể hoặc mã màu");
        }
        return colorStockRepository.findById(new ColorStockId(variantId.trim(), colorId.trim()))
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm không có màu này"));
    }

    private int requireQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Số lượng phải lớn hơn 0");
        }
        return quantity;
    }

    private void validateQuantity(int quantity, ColorStock stock) {
        if (quantity > currentStock(stock)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Số lượng vượt quá tồn kho");
        }
        if (quantity > maxQuantity(stock.getVariant().getProduct())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vượt quá số lượng tối đa cho phép mỗi đơn");
        }
    }

    private void checkSellable(ColorStock stock) {
        if (!isSellable(stock)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm đã ngừng bán");
        }
    }

    /** Còn bán = sản phẩm status = true và thuộc danh mục status = true. */
    private boolean isSellable(ColorStock stock) {
        Product product = stock.getVariant().getProduct();
        return product.isStatus() && product.getCategory().isStatus();
    }

    private int currentStock(ColorStock stock) {
        return stock.getStock() == null ? 0 : stock.getStock();
    }

    private int maxQuantity(Product product) {
        return product.getMaxQuantityPerOrder() == null ? DEFAULT_MAX_QUANTITY : product.getMaxQuantityPerOrder();
    }
}
