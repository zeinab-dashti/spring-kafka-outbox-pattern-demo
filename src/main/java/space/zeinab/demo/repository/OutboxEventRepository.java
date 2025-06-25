package space.zeinab.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import space.zeinab.demo.entity.OutboxEvent;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
