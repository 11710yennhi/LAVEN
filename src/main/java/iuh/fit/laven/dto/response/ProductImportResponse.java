package iuh.fit.laven.dto.response;

import iuh.fit.laven.dto.request.ProductImportRequest.Row;
import java.util.List;

public record ProductImportResponse(List<CreatedProduct> products) {
    public record CreatedProduct(int line, String productId) {}
    public record Problem(int line, String column, String message) {}
    public record Preview(List<Row> rows, List<Problem> errors) {}
}
