package com.aris.order.repository;

import com.aris.order.domain.OrderEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
