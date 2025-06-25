package space.zeinab.demo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import space.zeinab.demo.avro.OrderRequestEvent;
import space.zeinab.demo.service.OrderService;


@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final OrderService orderService;

    @KafkaListener(
            id = "order-listener", // this is needed for lookup in the ProducerMain class
            topics = "${app.input.topic.name}",
            groupId = "${app.consumer.group-id}"
    )
    public void listen(ConsumerRecord<String, OrderRequestEvent> record) {
        log.info("Received eventId={} from partition={} with value={}", record.key(), record.partition(), record.value());

        orderService.processOrder(record.value());

    }
}
