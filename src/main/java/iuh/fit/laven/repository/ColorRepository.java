package iuh.fit.laven.repository;

import iuh.fit.laven.model.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColorRepository extends JpaRepository<Color, String> {
    List<Color> findAllByOrderByNameAsc();
}
