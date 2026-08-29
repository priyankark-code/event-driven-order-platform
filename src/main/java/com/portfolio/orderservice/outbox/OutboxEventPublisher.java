package com.portfolio.orderservice.outbox;

import com.portfolio.orderservice.persistence.entity.OutboxEventEntity;
import com.portfolio.orderservice.persistence.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(
        name = "outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private static final String TOPIC = "order-events";

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventStateService stateService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, OutboxEventStateService stateService,
                                KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.stateService = stateService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEventEntity> events = findPendingEvents();

        for (OutboxEventEntity event : events) {
            publish(event);
        }
    }

    @Transactional(readOnly = true)
    public List<OutboxEventEntity> findPendingEvents() {
        return outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
    }

    private void publish(OutboxEventEntity event) {
        try {
            kafkaTemplate.send(
                    TOPIC,
                    event.getAggregateId().toString(),
                    event.getPayload()
            ).get(10, TimeUnit.SECONDS);

            stateService.markPublished(
                    event.getId(),
                    Instant.now()
            );

            log.info(
                    "Published outbox event {} for order {}",
                    event.getId(),
                    event.getAggregateId()
            );
        } catch (Exception exception) {
            log.error(
                    "Failed to publish outbox event {}",
                    event.getId(),
                    exception
            );
        }
    }
}