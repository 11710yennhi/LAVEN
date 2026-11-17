package iuh.fit.laven.repository;

import iuh.fit.laven.model.OrderDetail;
import iuh.fit.laven.model.OrderDetailId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, OrderDetailId> {
}
