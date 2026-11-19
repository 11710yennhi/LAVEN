package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.Category;
import lombok.Getter;

/** Danh mục công khai không trả chi phí đóng gói của cửa hàng. */
@Getter
public class CategoryResponse {
    private final String categoryId;
    private final String categoryName;
    private final String description;

    public CategoryResponse(Category category) {
        this.categoryId = category.getCategoryId();
        this.categoryName = category.getCategoryName();
        this.description = category.getDescription();
    }
}
