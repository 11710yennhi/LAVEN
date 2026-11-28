package iuh.fit.laven.repository;

import iuh.fit.laven.model.OrderDetail;
import iuh.fit.laven.model.id.OrderDetailId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, OrderDetailId> {

    List<OrderDetail> findAllById_OrderId(String idOrderId);

    List<OrderDetail> findAllById_OrderIdIn(Collection<String> orderIds);
}
