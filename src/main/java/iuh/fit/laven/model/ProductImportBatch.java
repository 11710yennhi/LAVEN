package iuh.fit.laven.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_import_batches")
@Getter
@NoArgsConstructor
public class ProductImportBatch {
    @Id
    @Column(length = 36)
    private String batchId;

    @Column(nullable = false, length = 64)
    private String payloadHash;

    @Column(nullable = false, length = 1200)
    private String productIds = "";

    public ProductImportBatch(String batchId, String payloadHash) {
        this.batchId = batchId;
        this.payloadHash = payloadHash;
    }

    public void recordProducts(String productIds) {
        this.productIds = productIds;
    }
}
