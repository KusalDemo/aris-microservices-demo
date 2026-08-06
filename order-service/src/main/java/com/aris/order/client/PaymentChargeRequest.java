package com.aris.order.client;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentChargeRequest(
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency
) {
}
