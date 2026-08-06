package com.aris.payment.service;

import com.aris.common.demo.DemoScenario;
import com.aris.payment.dto.ChargeRequest;
import com.aris.payment.dto.ChargeResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final Map<UUID, ChargeResponse> charges = new ConcurrentHashMap<>();

    public ChargeResponse charge(ChargeRequest request, DemoScenario scenario) {
        DemoScenario effective = normalize(scenario);
        log.info("Payment charge orderId={} scenario={}", request.orderId(), effective);

        switch (effective) {
            case PAYMENT_DOWN -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment dependency down");
            case PAYMENT_SLOW -> sleep(2500);
            case PARTNER_TIMEOUT -> sleep(10_000);
            case BUSY_SPIKE -> {
                sleep(ThreadLocalRandom.current().nextInt(100, 900));
                if (ThreadLocalRandom.current().nextDouble() < 0.35) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment busy spike");
                }
            }
            default -> {
                // NORMAL and order-only scenarios: fast success
            }
        }

        BigDecimal amount = request.amount() != null ? request.amount() : BigDecimal.ZERO;
        String currency = request.currency() != null ? request.currency() : "USD";
        ChargeResponse response = new ChargeResponse(
                UUID.randomUUID(),
                request.orderId(),
                "CHARGED",
                amount,
                currency,
                Instant.now()
        );
        charges.put(response.paymentId(), response);
        return response;
    }

    public Map<String, Object> health() {
        return Map.of(
                "service", "payment-service",
                "status", "UP",
                "chargesInMemory", charges.size()
        );
    }

    /**
     * Order-focused scenarios are ignored here (payment stays healthy).
     */
    private static DemoScenario normalize(DemoScenario scenario) {
        if (scenario == null) {
            return DemoScenario.NORMAL;
        }
        return switch (scenario) {
            case ORDER_SLOW, ORDER_DOWN, ORDER_DB_DOWN -> DemoScenario.NORMAL;
            default -> scenario;
        };
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment interrupted");
        }
    }
}
