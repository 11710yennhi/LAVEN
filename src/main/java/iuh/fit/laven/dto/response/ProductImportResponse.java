package iuh.fit.laven.dto.response;

import iuh.fit.laven.dto.request.ProductImportValidationRequest.Row;
import java.util.List;

public final class ProductImportResponse {
    private ProductImportResponse() {}
    public record Problem(int line, String column, String message) {}
    public record Preview(List<Row> rows, List<Problem> errors) {}
}
