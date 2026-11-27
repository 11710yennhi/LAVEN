package iuh.fit.laven.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.laven.dto.response.CategoryResponse;
import iuh.fit.laven.dto.response.ProductResponse;
import iuh.fit.laven.dto.response.SalePriceRange;
import iuh.fit.laven.dto.response.StaffProductResponse;
import iuh.fit.laven.dto.response.StaffVariantResponse;
import iuh.fit.laven.dto.response.VariantResponse;
import iuh.fit.laven.model.Category;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.id.ColorStockId;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.ShipCost;
import iuh.fit.laven.model.Variant;
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
    void publicProductAndCategoryDoNotExposePurchasePrice() throws Exception {
        Category category = new Category("CAT001", "Balo", "", 20_000, true);
        Product product = new Product();
        product.setProductId("PRO2026111910000001");
        product.setProductName("Balo A");
        product.setCategory(category);
        product.setMaxQuantityPerOrder(5);

        Variant variant = new Variant();
        variant.setVariantId("VAR2026111910000001");
        variant.setProduct(product);
        variant.setSize("20 inch");
        variant.setOriginalPrice(100_000);
        // ceil((100.000 + 20.000) / 0,7) làm tròn lên nghìn = 172.000
        assertEquals(172_000, variant.getSalePrice());

        SalePriceRange range = new SalePriceRange(variant.getSalePrice(), variant.getSalePrice());
        JsonNode publicJson = mapper.valueToTree(new ProductResponse(product, range));
        assertFalse(publicJson.has("originalPrice"));
        assertFalse(publicJson.has("packagingCost"));
        assertEquals(172_000, publicJson.get("minSalePrice").intValue());
        assertTrue(mapper.valueToTree(new StaffProductResponse(product, range)).has("packagingCost"));
        assertFalse(mapper.valueToTree(new CategoryResponse(category)).has("packagingCost"));

        // Giá nhập theo size: khách không thấy, staff thấy; entity Variant không bao giờ trả giá nhập ra JSON
        JsonNode publicVariant = mapper.valueToTree(new VariantResponse(variant));
        assertFalse(publicVariant.has("originalPrice"));
        assertEquals(172_000, publicVariant.get("salePrice").intValue());
        assertTrue(mapper.valueToTree(new StaffVariantResponse(variant)).has("originalPrice"));
        assertFalse(mapper.valueToTree(variant).has("originalPrice"));

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
        request.setMaxQuantityPerOrder(5);

        assertTrue(service.create(request).getProductId().matches("PRO\\d{16}"));
    }

    @Test
    void stockCreationCannotOverwriteAnExistingRow() {
        ColorStockRepository repository = mock(ColorStockRepository.class);
        ColorStockId id = new ColorStockId("VAR001", "#000000");
        when(repository.existsById(id)).thenReturn(true);
        ColorStockService service = new ColorStockService(repository, null, null, null, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.createStock("VAR001", "#000000", 99));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any(ColorStock.class));
    }
}
