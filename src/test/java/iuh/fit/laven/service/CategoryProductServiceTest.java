package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.CategoryProductStatusRequest;
import iuh.fit.laven.model.Category;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryProductServiceTest {
    private final CategoryService categories = mock(CategoryService.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final CategoryProductService service = new CategoryProductService(categories, products);

    @Test
    void selectAllUpdatesOnlyProductsThatWereNotDeselected() {
        Product selected = sellingProduct("PRO001");
        Product deselected = sellingProduct("PRO002");
        matchSellingProducts(selected, deselected);

        int updated = service.updateStatus("CAT001", stopAllExcept("PRO002"));

        assertEquals(1, updated);
        assertFalse(selected.isStatus());
        assertEquals("Chờ hàng", selected.getStopSellingReason());
        assertTrue(deselected.isStatus());
    }

    @Test
    void refusesToUpdateWhenAnExcludedProductNoLongerMatches() {
        Product selected = sellingProduct("PRO001");
        matchSellingProducts(selected);

        var error = assertThrows(ResponseStatusException.class,
            () -> service.updateStatus("CAT001", stopAllExcept("PRO002")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(selected.isStatus());
    }

    private void matchSellingProducts(Product... matching) {
        Category category = new Category();
        category.setStatus(true);
        when(categories.findById("CAT001", true)).thenReturn(category);
        when(products.findCategoryProductsForStatusUpdate("CAT001", "sakos", true)).thenReturn(List.of(matching));
    }

    private Product sellingProduct(String id) {
        Product product = new Product();
        product.setProductId(id);
        product.setStatus(true);
        return product;
    }

    private CategoryProductStatusRequest stopAllExcept(String excludedId) {
        return new CategoryProductStatusRequest(true, null, List.of(excludedId), "sakos", "selling",
            false, "Chờ hàng", 1);
    }
}
