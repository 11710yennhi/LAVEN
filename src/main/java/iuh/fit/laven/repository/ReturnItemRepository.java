package iuh.fit.laven.repository;

import iuh.fit.laven.model.ReturnItem;
import iuh.fit.laven.model.ReturnItemId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, ReturnItemId> {
}
