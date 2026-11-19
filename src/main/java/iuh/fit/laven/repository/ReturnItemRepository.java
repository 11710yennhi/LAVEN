package iuh.fit.laven.repository;

import iuh.fit.laven.model.ReturnItem;
import iuh.fit.laven.model.id.ReturnItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, ReturnItemId> {

    List<ReturnItem> findAllById_OrderId(String orderId);
}
