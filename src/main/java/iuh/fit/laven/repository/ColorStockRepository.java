package iuh.fit.laven.repository;

import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.id.ColorStockId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ColorStockRepository extends JpaRepository<ColorStock, ColorStockId> {
    List<ColorStock> findByVariantProductProductId(String productId);

    interface ProductColor {
        String getProductId();
        String getColorId();
        String getName();
    }

    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT s.variant.product.productId AS productId, s.color.colorId AS colorId, s.color.name AS name
        FROM ColorStock s WHERE s.variant.product.productId IN :ids
        ORDER BY s.variant.product.productId, s.color.name, s.color.colorId
        """)
    List<ProductColor> findProductColors(@org.springframework.data.repository.query.Param("ids") List<String> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stock from ColorStock stock where stock.id = :id")
    Optional<ColorStock> findLocked(@Param("id") ColorStockId id);

    @Query("""
        SELECT cs FROM ColorStock cs
        WHERE (:includeInactive = true
               OR (cs.variant.product.status = true AND cs.variant.product.category.status = true))
          AND (:productId IS NULL OR cs.variant.product.productId = :productId)
          AND (:productName IS NULL OR LOWER(cs.variant.product.productName) LIKE LOWER(CONCAT('%', :productName, '%')))
          AND (:variantId IS NULL OR LOWER(cs.variant.variantId) LIKE LOWER(CONCAT('%', :variantId, '%')))
          AND (:size IS NULL OR LOWER(cs.variant.size) LIKE LOWER(CONCAT('%', :size, '%')))
          AND (:colorId IS NULL OR LOWER(cs.color.colorId) LIKE LOWER(CONCAT('%', :colorId, '%')))
          AND (:colorName IS NULL OR LOWER(cs.color.name) LIKE LOWER(CONCAT('%', :colorName, '%')))
          AND (:minStock IS NULL OR cs.stock >= :minStock)
          AND (:maxStock IS NULL OR cs.stock <= :maxStock)
        ORDER BY cs.variant.variantId ASC, cs.color.name ASC
        """)
    List<ColorStock> search(
        @Param("productId") String productId,
        @Param("productName") String productName,
        @Param("variantId") String variantId,
        @Param("size") String size,
        @Param("colorId") String colorId,
        @Param("colorName") String colorName,
        @Param("minStock") Integer minStock,
        @Param("maxStock") Integer maxStock,
        @Param("includeInactive") boolean includeInactive
    );
    Optional<ColorStock> findByVariant_VariantIdAndColor_ColorId(String variantVariantId, String colorColorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        	SELECT cs
        	FROM ColorStock cs
        	WHERE cs.variant.variantId = :variantId
          		AND cs.color.colorId = :colorId
    	""")
    Optional<ColorStock> findByVariantAndColorForUpdate(
            @Param("variantId") String variantId,
            @Param("colorId") String colorId
    );
}
