package iuh.fit.laven.service.ai;

import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.Variant;
import iuh.fit.laven.repository.ColorStockRepository;
import iuh.fit.laven.repository.VariantRepository;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chuyển 1 sản phẩm (Product + Variant + Color) thành 1 Document của Spring AI.
 * - text: nội dung để Gemini embed (KHÔNG có giá, vì giá đổi theo khuyến mãi).
 * - metadata: dữ liệu để lọc khi tìm kiếm (productId, categoryId, brand, status).
 */
@Component
public class ProductDocumentMapper {

    private final VariantRepository variantRepository;
    private final ColorStockRepository colorStockRepository;

    public ProductDocumentMapper(VariantRepository variantRepository,
                                 ColorStockRepository colorStockRepository) {
        this.variantRepository = variantRepository;
        this.colorStockRepository = colorStockRepository;
    }

    public Document toDocument(Product product) {
        String productId = product.getProductId();

        List<Variant> variants = variantRepository.findByProductProductId(productId);

        // Tên các màu của sản phẩm, bỏ trùng (1 màu có thể có ở nhiều size)
        List<String> colorNames = colorStockRepository.findByVariantProductProductId(productId)
                .stream()
                .map(stock -> stock.getColor().getName())
                .distinct()
                .sorted()
                .toList();

        Map<String, Object> metadata = Map.of(
                "productId", productId,
                "categoryId", product.getCategory().getCategoryId(),
                "brand", product.getBrand(),
                "status", product.isStatus());

        return new Document(documentId(productId), buildText(product, variants, colorNames), metadata);
    }

    /** Đoạn văn mô tả sản phẩm, đây là thứ được biến thành vector. */
    String buildText(Product product, List<Variant> variants, List<String> colorNames) {
        StringBuilder text = new StringBuilder();
        text.append("Sản phẩm: ").append(product.getProductDisplayName()).append('\n');
        appendLine(text, "Chất liệu", product.getMaterial());
        appendLine(text, "Chống nước", product.getWaterResistance());
        appendLine(text, "Bánh xe", product.getWheel());
        appendLine(text, "Khóa kéo", product.getZipper());
        appendLine(text, "Bảo hành", product.getWarranty());
        appendLine(text, "Mô tả", product.getDescription());

        if (!variants.isEmpty()) {
            text.append("Các kích thước:\n");
            for (Variant variant : variants) {
                text.append("- ").append(describeVariant(variant)).append('\n');
            }
        }
        if (!colorNames.isEmpty()) {
            text.append("Màu sắc: ").append(String.join(", ", colorNames)).append('\n');
        }
        return text.toString().trim();
    }

    /** Ví dụ: "20 inch: 55 x 40 x 20 cm, nặng 2.5 kg, dung tích 35 lít" */
    private String describeVariant(Variant variant) {
        List<String> parts = new ArrayList<>();
        if (variant.getLength() != null && variant.getWidth() != null && variant.getHeight() != null) {
            parts.add(format(variant.getLength()) + " x " + format(variant.getWidth())
                    + " x " + format(variant.getHeight()) + " cm");
        }
        if (variant.getWeight() != null) {
            parts.add("nặng " + format(variant.getWeight()) + " kg");
        }
        if (variant.getCapacity() != null) {
            parts.add("dung tích " + format(variant.getCapacity()) + " lít");
        }
        return parts.isEmpty() ? variant.getSize() : variant.getSize() + ": " + String.join(", ", parts);
    }

    private void appendLine(StringBuilder text, String label, String value) {
        if (value != null && !value.isBlank()) {
            text.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    /** 60.0 -> "60", 2.5 -> "2.5" cho dễ đọc */
    private String format(Double number) {
        return number % 1 == 0 ? String.valueOf(number.longValue()) : String.valueOf(number);
    }

    /**
     * Cột id của bảng product_vectors có kiểu UUID, còn productId là "PRO...".
     * Sinh UUID CỐ ĐỊNH từ productId: cùng sản phẩm luôn ra cùng UUID,
     * nên index lại sẽ ghi đè bản cũ chứ không tạo bản trùng.
     */
    public static String documentId(String productId) {
        return UUID.nameUUIDFromBytes(("product-" + productId).getBytes(StandardCharsets.UTF_8)).toString();
    }
}