package com.aris.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ChargeResponse(
        UUID paymentId,
        UUID orderId,
        String status,
        BigDecimal amount,
        String currency,
        Instant chargedAt
) {
}
