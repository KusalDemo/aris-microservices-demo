package com.aris.order.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentChargeResponse(
        UUID paymentId,
        UUID orderId,
        String status,
        BigDecimal amount,
        String currency,
        Instant chargedAt
) {
}
