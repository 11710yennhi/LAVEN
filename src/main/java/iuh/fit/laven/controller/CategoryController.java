package iuh.fit.laven.controller;

import iuh.fit.laven.model.Category;
import iuh.fit.laven.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/public/categories")
    public List<Category> getPublicCategories() {
        return categoryService.findAll(false);
    }

    @GetMapping("/staff/categories")
    public List<Category> getCategories() {
        return categoryService.findAll(true);
    }

    @GetMapping("/staff/categories/{categoryId}")
    public Category getCategory(@PathVariable String categoryId) {
        return categoryService.findById(categoryId, true);
    }

    @PostMapping("/staff/categories")
    public Category createCategory(@Valid @RequestBody Category category) {
        return categoryService.create(category);
    }

    @PutMapping("/staff/categories/{categoryId}")
    public Category updateCategory(
        @PathVariable String categoryId,
        @Valid @RequestBody Category category
    ) {
        return categoryService.update(categoryId, category);
    }

    @DeleteMapping("/staff/categories/{categoryId}")
    public Category deactivateCategory(@PathVariable String categoryId) {
        return categoryService.deactivate(categoryId);
    }
}
