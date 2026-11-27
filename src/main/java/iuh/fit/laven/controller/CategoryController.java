package iuh.fit.laven.controller;

import org.springframework.http.MediaType;
import iuh.fit.laven.dto.request.CategoryProductStatusRequest;
import iuh.fit.laven.dto.response.CategoryProductPage;
import iuh.fit.laven.dto.response.CategoryResponse;
import iuh.fit.laven.model.Category;
import iuh.fit.laven.service.CategoryService;
import iuh.fit.laven.service.CategoryProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoryController {
    private final CategoryService categoryService;

    private final CategoryProductService categoryProducts;

    public CategoryController(CategoryService categoryService, CategoryProductService categoryProducts) {
        this.categoryProducts = categoryProducts;
        this.categoryService = categoryService;
    }

    @GetMapping("/public/categories")
    public List<CategoryResponse> getPublicCategories(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String categoryName
    ) {
        return categoryService.search(keyword, categoryName, null, null, null, false)
            .stream().map(CategoryResponse::new).toList();
    }

    @GetMapping("/staff/categories")
    public List<Category> getCategories(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String categoryName,
        @RequestParam(required = false) Integer minPackagingCost,
        @RequestParam(required = false) Integer maxPackagingCost,
        @RequestParam(required = false) Boolean status
    ) {
        return categoryService.search(keyword, categoryName, minPackagingCost, maxPackagingCost, status, true);
    }

    @GetMapping("/staff/categories/{categoryId}")
    public Category getCategory(@PathVariable String categoryId) {
        return categoryService.findById(categoryId, true);
    }

    @GetMapping("/staff/categories/{categoryId}/products")
    public CategoryProductPage getCategoryProducts(@PathVariable String categoryId,
        @RequestParam(required = false) String brand, @RequestParam(defaultValue = "all") String status,
        @RequestParam(defaultValue = "0") int page) {
        return categoryProducts.list(categoryId, brand, status, page);
    }

    @PostMapping("/staff/categories/{categoryId}/products/status")
    public Map<String, Integer> updateProductStatuses(@PathVariable String categoryId,
        @Valid @RequestBody CategoryProductStatusRequest request) {
        return Map.of("updated", categoryProducts.updateStatus(categoryId, request));
    }

    @PostMapping("/staff/categories")
    public Category createCategory(@Valid @RequestBody Category category) {
        return categoryService.create(category);
    }

    @PutMapping("/staff/categories/{categoryId}")
    public Category updateCategory(
        @PathVariable String categoryId,
        @Valid @RequestBody Category category,
        @RequestParam(required = false) Boolean status
    ) {
        return categoryService.update(categoryId, category, status);
    }

    @DeleteMapping("/staff/categories/{categoryId}")
    public Category deactivateCategory(@PathVariable String categoryId) {
        return categoryService.deactivate(categoryId);
    }
}
