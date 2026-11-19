package iuh.fit.laven.service.ai;

import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý chỉ mục vector của sản phẩm trong bảng product_vectors.
 * - Sản phẩm đang bán: thêm/cập nhật (upsert) Document.
 * - Sản phẩm ngừng bán hoặc danh mục ngừng kinh doanh: xóa khỏi chỉ mục.
 */
@Slf4j
@Service
public class ProductIndexService {

    private final VectorStore vectorStore;
    private final ProductRepository productRepository;
    private final ProductDocumentMapper mapper;
    private final String apiKey;

    public ProductIndexService(VectorStore vectorStore,
                               ProductRepository productRepository,
                               ProductDocumentMapper mapper,
                               @Value("${spring.ai.google.genai.embedding.api-key:}") String apiKey) {
        this.vectorStore = vectorStore;
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.apiKey = apiKey;
    }

    /** Kết quả 1 lần đánh chỉ mục: số sản phẩm đã index và số đã xóa khỏi chỉ mục. */
    public record ReindexResult(int indexed, int removed) {
    }

    /** Đã khai báo GEMINI_API_KEY thật trong .env chưa. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"missing-key".equals(apiKey);
    }

    /** Sản phẩm đang bán = sản phẩm còn bán và danh mục còn kinh doanh. */
    public static boolean isSelling(Product product) {
        return product.isStatus() && product.getCategory().isStatus();
    }

    /**
     * Đánh lại chỉ mục toàn bộ sản phẩm.
     * Không đặt @Transactional: lời gọi Gemini có thể mất vài giây,
     * không nên giữ kết nối DB trong lúc chờ mạng.
     */
    public ReindexResult reindexAll() {
        requireConfigured();

        List<Document> documents = new ArrayList<>();
        List<String> removedIds = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            if (isSelling(product)) {
                documents.add(mapper.toDocument(product));
            } else {
                removedIds.add(ProductDocumentMapper.documentId(product.getProductId()));
            }
        }

        try {
            if (!removedIds.isEmpty()) {
                vectorStore.delete(removedIds);
            }
            if (!documents.isEmpty()) {
                // Spring AI gọi Gemini lấy vector rồi INSERT; trùng id thì ghi đè (upsert)
                vectorStore.add(documents);
            }
        } catch (RuntimeException e) {
            log.error("Đánh chỉ mục sản phẩm thất bại", e);
            throw new AppException(HttpStatus.BAD_GATEWAY,
                    "Không đánh được chỉ mục, kiểm tra GEMINI_API_KEY và kết nối mạng: " + e.getMessage());
        }

        log.info("Đã đánh chỉ mục {} sản phẩm, xóa {} sản phẩm ngừng bán", documents.size(), removedIds.size());
        return new ReindexResult(documents.size(), removedIds.size());
    }

    /** Cập nhật chỉ mục của 1 sản phẩm (dùng khi cần index ngay sau khi sửa sản phẩm). */
    public void indexProduct(Product product) {
        requireConfigured();
        if (isSelling(product)) {
            vectorStore.add(List.of(mapper.toDocument(product)));
        } else {
            removeProduct(product.getProductId());
        }
    }

    /** Xóa 1 sản phẩm khỏi chỉ mục. */
    public void removeProduct(String productId) {
        vectorStore.delete(List.of(ProductDocumentMapper.documentId(productId)));
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình GEMINI_API_KEY trong file .env");
        }
    }
}