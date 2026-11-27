package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.CartItemRequest;
import iuh.fit.laven.dto.response.CartItemResponse;
import iuh.fit.laven.dto.response.CartResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.Cart;
import iuh.fit.laven.model.CartItem;
import iuh.fit.laven.model.id.CartItemId;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.id.ColorStockId;
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
    private final UserEventService events;
    private final ProductImageService productImageService;

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
        events.record(customerId, stock.getVariant().getProduct().getProductId(), iuh.fit.laven.model.UserEvent.Type.ADD_TO_CART);
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
        CartItemId id = new CartItemId(cart.getCartId(), variantId.trim(), normalizeColorId(colorId));
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
                String key = g.getVariantId().trim() + "|" + normalizeColorId(g.getColorId());
                merged.merge(key, g.getQuantity(), Integer::sum);
                keys.putIfAbsent(key, g);
            }
        }

        for (Map.Entry<String, Integer> entry : merged.entrySet()) {
            CartItemRequest g = keys.get(entry.getKey());
            ColorStock stock = colorStockRepository
                    .findById(new ColorStockId(g.getVariantId().trim(), normalizeColorId(g.getColorId())))
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
        record PreviewEntry(String variantId, String colorId, int quantity, ColorStock stock) {}
        List<PreviewEntry> entries = new ArrayList<>();
        if (items != null) {
            for (CartItemRequest r : items) {
                if (r == null || r.getVariantId() == null || r.getColorId() == null) {
                    continue;
                }
                String variantId = r.getVariantId().trim();
                String colorId = normalizeColorId(r.getColorId());
                int quantity = r.getQuantity() == null ? 0 : r.getQuantity();
                ColorStock stock = colorStockRepository
                        .findById(new ColorStockId(variantId, colorId))
                        .orElse(null);
                entries.add(new PreviewEntry(variantId, colorId, quantity, stock));
            }
        }
        Map<String, String> covers = loadFallbackCovers(entries.stream().map(PreviewEntry::stock).toList());
        List<CartItemResponse> responses = entries.stream()
                .map(e -> buildItem(e.variantId(), e.colorId(), e.quantity(), e.stock(), promotions, covers))
                .toList();
        return toCartResponse(responses);
    }

    // =====================================================================
    // Nội bộ
    // =====================================================================
    private Cart getOrCreateCart(String customerId) {
        return cartRepository.findByCustomer_UserId(customerId).orElseGet(() -> {
            User customer = userRepository.findById(customerId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
            return cartRepository.save(
                    new Cart(IdGenerator.nextUnique("CRT", cartRepository::existsById), customer));
        });
    }

    private CartResponse buildCart(Cart cart) {
        List<ScopedPromotion> promotions = promotionService.loadActiveProductPromotions();
        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());
        List<ColorStock> stocks = new ArrayList<>(cartItems.size());
        for (CartItem item : cartItems) {
            stocks.add(colorStockRepository.findById(new ColorStockId(item.getId().getVariantId(), item.getId().getColorId())).orElse(null));
        }
        Map<String, String> covers = loadFallbackCovers(stocks);

        List<CartItemResponse> responses = new ArrayList<>(cartItems.size());
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            String variantId = item.getId().getVariantId();
            String colorId = item.getId().getColorId();
            ColorStock stock = stocks.get(i);
            CartItemResponse response = buildItem(variantId, colorId, item.getQuantity(), stock, promotions, covers);
            // Số lượng bị cắt về mức cho phép thì ghi lại vào giỏ
            if (response.getQuantity() > 0 && response.getQuantity() != item.getQuantity()) {
                item.setQuantity(response.getQuantity());
                cartItemRepository.save(item);
            }
            responses.add(response);
        }
        return toCartResponse(responses);
    }

    private Map<String, String> loadFallbackCovers(List<ColorStock> stocks) {
        List<String> productIds = stocks.stream()
                .filter(s -> s != null && (s.getImageUrl() == null || s.getImageUrl().isBlank()))
                .map(s -> s.getVariant().getProduct().getProductId())
                .distinct()
                .toList();
        return productIds.isEmpty() ? Map.of() : productImageService.findCoverUrls(productIds);
    }

    private CartItemResponse buildItem(String variantId, String colorId, int quantity,
                                       ColorStock stock, List<ScopedPromotion> promotions,
                                       Map<String, String> covers) {
        if (stock == null) {
            return CartItemResponse.builder()
                    .variantId(variantId).colorId(colorId).quantity(quantity)
                    .available(false).warning("Sản phẩm không có màu này")
                    .build();
        }
        Product product = stock.getVariant().getProduct();
        int salePrice = stock.getVariant().getSalePrice(); // giá theo size
        int discount = promotionService.findBestProductPromotion(product, salePrice, promotions)
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

        String imageUrl = stock.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = covers.get(product.getProductId());
        }

        return CartItemResponse.builder()
                .variantId(variantId)
                .colorId(colorId)
                .colorName(stock.getColor().getName())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .categoryName(product.getCategory().getCategoryName())
                .imageUrl(imageUrl)
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
            item = new CartItem(cart, stock.getVariant(), stock.getColor(), quantity);
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
        return colorStockRepository.findById(new ColorStockId(variantId.trim(), normalizeColorId(colorId)))
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

    /** Mã màu lưu chữ hoa trong DB (#093F69), nên chuẩn hóa để client gửi chữ thường vẫn khớp. */
    private String normalizeColorId(String colorId) {
        return colorId == null ? null : colorId.trim().toUpperCase();
    }
}
