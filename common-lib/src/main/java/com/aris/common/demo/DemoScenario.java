package com.aris.common.demo;

import java.util.Locale;

/**
 * Fault / load scenario applied by order-service and/or payment-service.
 */
public enum DemoScenario {
    NORMAL,
    BUSY_SPIKE,
    PAYMENT_SLOW,
    PAYMENT_DOWN,
    ORDER_SLOW,
    ORDER_DOWN,
    ORDER_DB_DOWN,
    PARTNER_TIMEOUT;

    public static DemoScenario fromHeader(String raw) {
        if (raw == null || raw.isBlank()) {
            return NORMAL;
        }
        return DemoScenario.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    public boolean isPaymentFocused() {
        return this == PAYMENT_SLOW
                || this == PAYMENT_DOWN
                || this == PARTNER_TIMEOUT
                || this == BUSY_SPIKE;
    }

    public boolean isOrderFocused() {
        return this == ORDER_SLOW
                || this == ORDER_DOWN
                || this == ORDER_DB_DOWN
                || this == BUSY_SPIKE;
    }
}
