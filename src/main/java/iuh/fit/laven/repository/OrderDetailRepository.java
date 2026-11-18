package iuh.fit.laven.repository;

import iuh.fit.laven.model.OrderDetail;
import iuh.fit.laven.model.OrderDetailId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, OrderDetailId> {

    List<OrderDetail> findAllById_OrderId(String idOrderId);
}
