package space.zeinab.demo.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import space.zeinab.demo.avro.OrderRequestEvent;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderProducer implements SmartLifecycle {

    private final KafkaTemplate<String, OrderRequestEvent> orderRequestKafkaTemplate;
    private final KafkaListenerEndpointRegistry registry;

    @Value("${app.input.topic.name}")
    private String inputTopicName;

    private boolean isRunning = false;

    @Override
    public void start() {
        var container = registry.getListenerContainer("order-listener");

        if (container != null) {
            try {
                // Wait for full consumer readiness (thread + assignment)
                while (!container.isRunning() || container.getAssignedPartitions().isEmpty()) {
                    Thread.sleep(200);
                }

                System.out.println("Kafka listener is fully ready. Now sending events.");

                List<OrderRequestEvent> samples = List.of(
                        OrderRequestEvent.newBuilder()
                                .setEventId("evt-1")
                                .setProductCode("P-001")
                                .setQuantity(2)
                                .setTimestamp(System.currentTimeMillis())
                                .build(),
                        OrderRequestEvent.newBuilder()
                                .setEventId("evt-2")
                                .setProductCode("P-002")
                                .setQuantity(5)
                                .setTimestamp(System.currentTimeMillis())
                                .build(),
                        OrderRequestEvent.newBuilder()
                                .setEventId("evt-3")
                                .setProductCode("P-003")
                                .setQuantity(1)
                                .setTimestamp(System.currentTimeMillis())
                                .build()
                );

                samples.forEach(orderRequestEvent ->
                        orderRequestKafkaTemplate.send(inputTopicName, orderRequestEvent.getEventId().toString(), orderRequestEvent)
                                .whenComplete((result, ex) -> {
                                    if (ex != null) {
                                        System.err.println("Failed to send: " + orderRequestEvent);
                                        ex.printStackTrace();
                                    } else {
                                        System.out.println("Sent: " + orderRequestEvent);
                                    }
                                })
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        isRunning = true;
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}