package com.portfolio.orderservice.persistence.repository;

import com.portfolio.orderservice.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

    long countByPublishedAtIsNull();
}