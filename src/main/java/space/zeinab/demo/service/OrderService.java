package space.zeinab.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.zeinab.demo.avro.OrderProcessedEvent;
import space.zeinab.demo.avro.OrderRequestEvent;
import space.zeinab.demo.entity.OrderEntity;
import space.zeinab.demo.entity.OutboxEvent;
import space.zeinab.demo.repository.OrderRepository;
import space.zeinab.demo.repository.OutboxEventRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OutboxEventRepository outboxRepo;

    @Transactional
    public void processOrder(OrderRequestEvent req) {
        // Persist business record
        OrderEntity e = new OrderEntity();
        e.setEventId(req.getEventId().toString());
        e.setProductCode(req.getProductCode().toString());
        e.setQuantity(req.getQuantity());
        e.setCreatedAt(req.getTimestamp());
        orderRepo.save(e);

        // Build and serialize processed-event
        OrderProcessedEvent proc = OrderProcessedEvent.newBuilder()
                .setEventId(req.getEventId())
                .setStatus("PROCESSED")
                .setProcessedAt(System.currentTimeMillis())
                .build();

        // Persist outbox table
        OutboxEvent o = new OutboxEvent();
        o.setId(UUID.randomUUID());
        o.setAggregateId(req.getEventId().toString());
        o.setEventType("OrderRequestEvent");
        o.setOccurredAt(Instant.ofEpochMilli(req.getTimestamp()));
        o.setPayload(AvroJson.toJson(proc));

        outboxRepo.save(o);
    }
}
