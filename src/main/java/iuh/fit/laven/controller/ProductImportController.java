package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.ProductImportValidationRequest;
import iuh.fit.laven.dto.response.ProductImportResponse.Problem;
import iuh.fit.laven.dto.response.ProductImportResponse.Preview;
import iuh.fit.laven.service.ProductImportFileService;
import iuh.fit.laven.service.ProductImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/staff/products/import")
@RequiredArgsConstructor
public class ProductImportController {
    private final ProductImportFileService files;
    private final ProductImportService products;

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam String format) {
        var file = files.template(format);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.name()).build().toString())
            .body(file.content());
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Preview preview(@RequestParam("file") MultipartFile file) {
        var parsed = files.parse(file);
        return products.preview(parsed);
    }

    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<Problem> validate(@Valid @RequestBody ProductImportValidationRequest request) {
        return products.validateForImport(request.rows());
    }
}
