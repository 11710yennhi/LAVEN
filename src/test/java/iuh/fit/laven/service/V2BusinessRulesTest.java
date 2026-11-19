package iuh.fit.laven.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.laven.dto.response.CartItemResponse;
import iuh.fit.laven.dto.response.CategoryResponse;
import iuh.fit.laven.dto.response.ProductResponse;
import iuh.fit.laven.dto.response.StaffProductResponse;
import iuh.fit.laven.model.Category;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.ColorStockId;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.ShipCost;
import iuh.fit.laven.repository.CategoryRepository;
import iuh.fit.laven.repository.ColorStockRepository;
import iuh.fit.laven.repository.ProductRepository;
import iuh.fit.laven.repository.ShipCostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class V2BusinessRulesTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void publicProductAndCartDoNotExposePurchasePrice() throws Exception {
        Category category = new Category("CAT001", "Balo", "", 20_000, true);
        Product product = new Product();
        product.setProductId("PRO2026111910000001");
        product.setProductName("Balo A");
        product.setCategory(category);
        product.setOriginalPrice(100_000);
        product.setMaxQuantityPerOrder(5);

        JsonNode publicJson = mapper.valueToTree(new ProductResponse(product));
        assertFalse(publicJson.has("originalPrice"));
        assertFalse(publicJson.has("packagingCost"));
        assertEquals(product.getSalePrice(), publicJson.get("salePrice").intValue());
        assertTrue(mapper.valueToTree(new StaffProductResponse(product)).has("originalPrice"));
        assertFalse(mapper.valueToTree(new CategoryResponse(category)).has("packagingCost"));

        CartItemResponse cartItem = CartItemResponse.builder().salePrice(172_000).build();
        JsonNode cartJson = mapper.valueToTree(cartItem);
        assertTrue(cartJson.has("salePrice"));
        assertFalse(cartJson.has("originalPrice"));
    }

    @Test
    void shipCostKeepsProvidedProvinceIdAndCity() {
        ShipCostRepository repository = mock(ShipCostRepository.class);
        when(repository.save(any(ShipCost.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ShipCostService service = new ShipCostService(repository);
        ShipCost created = service.create(new ShipCost(2, "Hồ Chí Minh", 30_000));
        assertEquals(2, created.getShipCostId());

        when(repository.findById(2)).thenReturn(Optional.of(created));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.update(2, new ShipCost(2, "Tên tỉnh mới", 35_000)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void newProductIdUsesProPrefix() {
        ProductRepository products = mock(ProductRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        Category category = new Category("CAT001", "Balo", "", 20_000, true);
        when(categories.findById("CAT001")).thenReturn(Optional.of(category));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProductService service = new ProductService(products, categories, null, null);
        Product request = new Product();
        request.setCategory(category);
        request.setOriginalPrice(100_000);
        request.setMaxQuantityPerOrder(5);

        assertTrue(service.create(request).getProductId().matches("PRO\\d{16}"));
    }

    @Test
    void stockCreationCannotOverwriteAnExistingRow() {
        ColorStockRepository repository = mock(ColorStockRepository.class);
        ColorStockId id = new ColorStockId("VAR001", "#000000");
        when(repository.existsById(id)).thenReturn(true);
        ColorStockService service = new ColorStockService(repository, null, null, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.createStock("VAR001", "#000000", 99));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any(ColorStock.class));
    }
}
