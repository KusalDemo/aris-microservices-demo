package com.aris.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        String itemName,
        BigDecimal amount,
        String currency,
        String status,
        UUID paymentId,
        Instant createdAt
) {
}
