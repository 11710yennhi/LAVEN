package iuh.fit.laven.service;

import iuh.fit.laven.model.Category;
import iuh.fit.laven.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll(boolean includeInactive) {
        return includeInactive
            ? categoryRepository.findAll()
            : categoryRepository.findByStatusTrueOrderByCategoryNameAsc();
    }

    @Transactional(readOnly = true)
    public Category findById(String id, boolean includeInactive) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> notFound());
        if (!includeInactive && !category.isStatus()) {
            throw notFound();
        }
        return category;
    }

    public Category create(Category category) {
        if (category.getCategoryId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã danh mục do hệ thống sinh");
        }
        if (categoryRepository.existsByCategoryNameIgnoreCase(category.getCategoryName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên danh mục đã tồn tại");
        }

        int maxId = categoryRepository.findAll().stream()
            .map(Category::getCategoryId)
            .filter(id -> id != null && id.matches("CAT\\d{3}"))
            .map(id -> Integer.parseInt(id.substring(3)))
            .max(Integer::compareTo)
            .orElse(0);

        category.setCategoryId("CAT" + String.format("%03d", maxId + 1));
        category.setStatus(true);
        return categoryRepository.save(category);
    }

    public Category update(String id, Category request) {
        Category category = findById(id, true);
        if (!category.getCategoryName().equalsIgnoreCase(request.getCategoryName())
            && categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(
                request.getCategoryName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên danh mục đã tồn tại");
        }

        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setPackagingCost(request.getPackagingCost());
        return categoryRepository.save(category);
    }

    public Category deactivate(String id) {
        Category category = findById(id, true);
        category.setStatus(false);
        return categoryRepository.save(category);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại");
    }
}
