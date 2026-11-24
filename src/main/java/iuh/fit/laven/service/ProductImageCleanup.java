package iuh.fit.laven.service;

import iuh.fit.laven.repository.CategoryRepository;
import iuh.fit.laven.repository.ColorStockRepository;
import iuh.fit.laven.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductImageCleanup {
    private final ProductImageRepository images;
    private final ColorStockRepository stocks;
    private final CategoryRepository categories;
    private final ProductImageCloudinaryService cloudinary;

    public record RemovedImage(String imageUrl) {}

    @TransactionalEventListener
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void deleteUnusedImage(RemovedImage event) {
        String url = event.imageUrl();
        try {
            if (url != null && !images.existsByImageUrl(url) && !stocks.existsByImageUrl(url)
                && !categories.existsByImageUrl(url)) {
                cloudinary.deleteByUrl(url);
            }
        } catch (RuntimeException error) {
            log.warn("Không dọn được ảnh sản phẩm sau khi cập nhật MariaDB: {}", url, error);
        }
    }
}
