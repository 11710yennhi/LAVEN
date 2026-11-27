package iuh.fit.laven.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.laven.dto.request.ProductImportRequest;
import iuh.fit.laven.dto.request.ProductImportRequest.Row;
import iuh.fit.laven.dto.response.ProductImportResponse;
import iuh.fit.laven.dto.response.ProductImportResponse.CreatedProduct;
import iuh.fit.laven.dto.response.ProductImportResponse.Preview;
import iuh.fit.laven.dto.response.ProductImportResponse.Problem;
import iuh.fit.laven.model.Category;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.ProductImportBatch;
import iuh.fit.laven.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImportService {
    private final ProductService products;
    private final CategoryRepository categories;
    private final EntityManager entityManager;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public Preview preview(Preview parsed) {
        var errors = new ArrayList<>(parsed.errors());
        errors.addAll(validate(parsed.rows(), false));
        return new Preview(parsed.rows(), errors);
    }

    @Transactional
    public ProductImportResponse confirm(ProductImportRequest request) {
        String batchId = request.batchId().toString();
        String hash = payloadHash(request.rows());
        var existing = entityManager.find(ProductImportBatch.class, batchId);
        if (existing != null) {
            if (!existing.getPayloadHash().equals(hash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Nội dung đã thay đổi. Hãy bắt đầu đợt nhập mới");
            }
            return result(request.rows(), List.of(existing.getProductIds().split(",")));
        }

        var errors = validate(request.rows(), true);
        if (!errors.isEmpty()) {
            var first = errors.getFirst();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Dòng " + first.line() + " · " + first.column() + " · " + first.message());
        }
        // Khóa theo mã đợt trong cùng giao dịch. Gửi lại sau lỗi mạng sẽ dùng lại các mã PRO đã tạo.
        var batch = new ProductImportBatch(batchId, hash);
        entityManager.persist(batch);
        entityManager.flush();

        var ids = new ArrayList<String>();
        for (Row row : request.rows()) {
            Product product = products.create(toProduct(row));
            ids.add(product.getProductId());
        }
        batch.recordProducts(String.join(",", ids));
        return result(request.rows(), ids);
    }

    private ProductImportResponse result(List<Row> rows, List<String> ids) {
        var created = new ArrayList<CreatedProduct>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            created.add(new CreatedProduct(rows.get(i).line(), ids.get(i)));
        }
        return new ProductImportResponse(created);
    }

    private List<Problem> validate(List<Row> rows, boolean confirming) {
        var errors = new ArrayList<Problem>();
        var activeCategories = categories.findAllById(rows.stream().map(Row::categoryId)
                .filter(id -> id != null && !id.isBlank()).map(this::categoryId).distinct().toList())
            .stream().filter(Category::isStatus).map(Category::getCategoryId)
            .collect(Collectors.toSet());
        var lineNumbers = new HashSet<Integer>();

        for (Row row : rows) {
            int line = row.line();
            if (line < 2 || !lineNumbers.add(line)) {
                errors.add(new Problem(line, "File", "Số dòng không hợp lệ hoặc bị trùng"));
            }
            check(errors, line, "productName", row.productName(), 200, true);
            check(errors, line, "categoryId", row.categoryId(), 6, true);
            check(errors, line, "brand", row.brand(), 100, true);
            check(errors, line, "origin", row.origin(), 100, true);
            check(errors, line, "material", row.material(), 150, true);
            check(errors, line, "description", row.description(), 2000, false);
            check(errors, line, "waterResistance", row.waterResistance(), 100, false);
            check(errors, line, "wheel", row.wheel(), 100, false);
            check(errors, line, "zipper", row.zipper(), 100, false);
            check(errors, line, "warranty", row.warranty(), 100, false);
            check(errors, line, "stopSellingReason", row.stopSellingReason(), 500,
                confirming && Boolean.FALSE.equals(row.status()));
            if (row.categoryId() != null && !row.categoryId().isBlank()
                    && !activeCategories.contains(categoryId(row.categoryId()))) {
                errors.add(new Problem(line, "categoryId", "Danh mục không tồn tại hoặc đã ngừng kinh doanh"));
            }
            if (row.maxQuantityPerOrder() != null && row.maxQuantityPerOrder() < 1) {
                errors.add(new Problem(line, "maxQuantityPerOrder", "Giới hạn mua phải lớn hơn 0"));
            }
        }
        return errors;
    }

    private void check(List<Problem> errors, int line, String column, String value, int max, boolean required) {
        if (required && (value == null || value.isBlank())) {
            errors.add(new Problem(line, column, "Cần nhập giá trị"));
        } else if (value != null && value.strip().length() > max) {
            errors.add(new Problem(line, column, "Tối đa " + max + " ký tự"));
        }
    }

    private Product toProduct(Row row) {
        var product = new Product();
        var category = new Category();
        category.setCategoryId(categoryId(row.categoryId()));
        product.setCategory(category);
        product.setProductName(row.productName().strip());
        product.setBrand(row.brand().strip());
        product.setOrigin(row.origin().strip());
        product.setMaterial(row.material().strip());
        product.setStatus(!Boolean.FALSE.equals(row.status()));
        product.setStopSellingReason(row.stopSellingReason());
        product.setDescription(clean(row.description()));
        product.setWaterResistance(clean(row.waterResistance()));
        product.setWheel(clean(row.wheel()));
        product.setZipper(clean(row.zipper()));
        product.setWarranty(clean(row.warranty()));
        product.setMaxQuantityPerOrder(row.maxQuantityPerOrder() == null ? 5 : row.maxQuantityPerOrder());
        return product;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String categoryId(String id) {
        return id.strip().toUpperCase(Locale.ROOT);
    }

    private String payloadHash(List<Row> rows) {
        try {
            byte[] input = mapper.writeValueAsBytes(rows);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Không đọc được dữ liệu nhập sản phẩm", ex);
        }
    }
}
