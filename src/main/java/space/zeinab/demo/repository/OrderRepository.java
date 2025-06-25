package space.zeinab.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import space.zeinab.demo.entity.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
}