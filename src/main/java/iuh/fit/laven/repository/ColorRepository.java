package iuh.fit.laven.repository;

import iuh.fit.laven.model.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ColorRepository extends JpaRepository<Color, String> {
    List<Color> findAllByOrderByNameAsc();

    @Query("""
        SELECT c FROM Color c
        WHERE (:keyword IS NULL
               OR LOWER(c.colorId) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:colorId IS NULL OR LOWER(c.colorId) LIKE LOWER(CONCAT('%', :colorId, '%')))
          AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
        ORDER BY c.name ASC
        """)
    List<Color> search(
        @Param("keyword") String keyword,
        @Param("colorId") String colorId,
        @Param("name") String name
    );
}
