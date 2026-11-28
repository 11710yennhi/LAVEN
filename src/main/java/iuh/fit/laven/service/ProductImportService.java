package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.ProductImportValidationRequest.Row;
import iuh.fit.laven.dto.response.ProductImportResponse.Preview;
import iuh.fit.laven.dto.response.ProductImportResponse.Problem;
import iuh.fit.laven.model.Category;
import iuh.fit.laven.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImportService {
    private final CategoryRepository categories;

    @Transactional(readOnly = true)
    public Preview preview(Preview parsed) {
        var errors = new ArrayList<>(parsed.errors());
        errors.addAll(validate(parsed.rows(), false));
        return new Preview(parsed.rows(), errors);
    }

    @Transactional(readOnly = true)
    public List<Problem> validateForImport(List<Row> rows) {
        return validate(rows, true);
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

    private String categoryId(String id) {
        return id.strip().toUpperCase(Locale.ROOT);
    }
}
