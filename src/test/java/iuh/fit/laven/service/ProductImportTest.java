package iuh.fit.laven.service;

import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import iuh.fit.laven.dto.request.ProductImportValidationRequest.Row;
import iuh.fit.laven.dto.response.ProductImportResponse.Problem;
import iuh.fit.laven.model.Category;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.repository.CategoryRepository;
import iuh.fit.laven.repository.ProductRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductImportTest {
    private final ProductImportFileService files = new ProductImportFileService();

    @Test
    void csvKeepsVietnameseAndReportsPhysicalRowAfterMultilineValue() {
        String content = "\uFEFFproductName,categoryId,brand,origin,material,description,status\n"
            + "Túi xách,CAT003,LAVEN,Việt Nam,Vải,\"Mô tả, dòng một\nDòng hai\",true\n"
            + "Balo,CAT002,LAVEN,Việt Nam,Vải,,sai\n";
        var result = files.parse(new MockMultipartFile("file", "san-pham.csv", "text/csv",
            content.getBytes(StandardCharsets.UTF_8)));
        assertEquals(List.of(2, 4), result.rows().stream().map(Row::line).toList());
        assertEquals("Túi xách", result.rows().getFirst().productName());
        assertEquals("Mô tả, dòng một\nDòng hai", result.rows().getFirst().description());
        assertEquals(5, result.rows().getFirst().maxQuantityPerOrder());
        assertEquals("status", result.errors().getFirst().column());
        assertEquals(4, result.errors().getFirst().line());
    }

    @Test
    void excelTemplateAcceptsOneProductPerRowAndBooleanStatus() throws Exception {
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(files.template("xlsx").content()));
             var output = new ByteArrayOutputStream()) {
            var row = workbook.getSheet("SanPham").createRow(1);
            row.createCell(0).setCellValue("Vali kéo");
            row.createCell(1).setCellValue("CAT001");
            row.createCell(2).setCellValue("LAVEN");
            row.createCell(3).setCellValue("Việt Nam");
            row.createCell(4).setCellValue("Nhựa");
            row.createCell(10).setCellValue(8);
            row.createCell(11).setCellValue(false);
            workbook.write(output);
            var result = files.parse(new MockMultipartFile("file", "san-pham.xlsx", "", output.toByteArray()));
            assertTrue(result.errors().isEmpty());
            assertEquals(1, result.rows().size());
            assertEquals(8, result.rows().getFirst().maxQuantityPerOrder());
            assertFalse(result.rows().getFirst().status());
        }
    }

    @Test
    void validationReturnsAllInvalidRowsBeforeAnyProductIsCreated() {
        var categories = mock(CategoryRepository.class);
        when(categories.findAllById(List.of("CAT001", "CAT002")))
            .thenReturn(List.of(new Category("CAT001", "Vali", "", 35_000, true)));

        var service = new ProductImportService(categories);
        var invalidCategoryAndQuantity = new Row(4, "Balo", "CAT002", "LAVEN", "Việt Nam", "Vải",
            null, null, null, null, null, 0, true, null);
        var errors = service.validateForImport(List.of(row(null), invalidCategoryAndQuantity));
        assertEquals(List.of(2, 4, 4), errors.stream().map(Problem::line).toList());
        assertEquals(List.of("stopSellingReason", "categoryId", "maxQuantityPerOrder"),
            errors.stream().map(Problem::column).toList());
    }

    @Test
    void validationAcceptsValidRows() {
        var categories = mock(CategoryRepository.class);
        when(categories.findAllById(List.of("CAT001")))
            .thenReturn(List.of(new Category("CAT001", "Vali", "", 35_000, true)));
        var service = new ProductImportService(categories);
        assertTrue(service.validateForImport(List.of(row("Chờ hàng"))).isEmpty());
    }

    @Test
    void partialEditCanClearOneFieldAndLeavesUnsentFieldsAndCoverIntact() {
        var repository = mock(ProductRepository.class);
        var product = new Product();
        product.setBrand("Thương hiệu hiện tại");
        product.setDescription("Mô tả cũ");
        product.setStatus(true);
        product.setCoverImageId("IMG001");
        when(repository.findByProductIdForUpdate("PRO001")).thenReturn(Optional.of(product));

        new ProductService(repository, null, null, null).patch("PRO001",
            Map.of("description", NullNode.getInstance(), "productName", TextNode.valueOf("Tên mới")));

        assertEquals("Tên mới", product.getProductName());
        assertNull(product.getDescription());
        assertEquals("Thương hiệu hiện tại", product.getBrand());
        assertEquals("IMG001", product.getCoverImageId());
        assertTrue(product.isStatus());
    }

    private Row row(String reason) {
        return new Row(2, "Vali", "CAT001", "LAVEN", "Việt Nam", "Nhựa",
            null, null, null, null, null, 5, false, reason);
    }
}
