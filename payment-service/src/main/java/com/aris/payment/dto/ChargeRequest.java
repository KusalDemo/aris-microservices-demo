package com.aris.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ChargeRequest(
        @NotNull UUID orderId,
        @NotNull UUID userId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String currency
) {
}
