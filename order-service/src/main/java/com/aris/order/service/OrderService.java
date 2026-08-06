package com.aris.order.service;

import com.aris.common.aris.ArisDecideResponse;
import com.aris.common.aris.ArisHttpResult;
import com.aris.common.demo.DemoPolicyMode;
import com.aris.common.demo.DemoScenario;
import com.aris.order.client.PaymentChargeRequest;
import com.aris.order.client.PaymentChargeResponse;
import com.aris.order.client.PaymentClient;
import com.aris.order.domain.OrderEntity;
import com.aris.order.dto.OrderResponse;
import com.aris.order.dto.PlaceOrderRequest;
import com.aris.order.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
    private final OrderScenarioBehaviour scenarioBehaviour;
    private final DemoStatsService demoStatsService;
    private final ErrorRateTracker errorRateTracker;

    public OrderService(
            OrderRepository orderRepository,
            PaymentClient paymentClient,
            OrderScenarioBehaviour scenarioBehaviour,
            DemoStatsService demoStatsService,
            ErrorRateTracker errorRateTracker
    ) {
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
        this.scenarioBehaviour = scenarioBehaviour;
        this.demoStatsService = demoStatsService;
        this.errorRateTracker = errorRateTracker;
    }

    @Transactional
    public OrderResponse placeOrder(
            UUID userId,
            PlaceOrderRequest request,
            DemoPolicyMode policyMode,
            DemoScenario scenario,
            String authorizationHeader
    ) {
        demoStatsService.recordStart(policyMode, scenario);
        ArisDecideResponse lastDecision = null;

        try {
            scenarioBehaviour.beforePersistence(scenario);
            scenarioBehaviour.assertDbAvailable(scenario);

            OrderEntity order = new OrderEntity();
            order.setId(UUID.randomUUID());
            order.setUserId(userId);
            order.setItemName(request.itemName().trim());
            order.setAmount(request.amount());
            order.setCurrency(request.currency() == null || request.currency().isBlank() ? "USD" : request.currency());
            order.setStatus("PENDING_PAYMENT");
            order.setCreatedAt(Instant.now());
            orderRepository.save(order);

            PaymentChargeRequest chargeRequest = new PaymentChargeRequest(
                    order.getId(),
                    userId,
                    order.getAmount(),
                    order.getCurrency()
            );

            ArisHttpResult<PaymentChargeResponse> result = paymentClient.charge(
                    chargeRequest,
                    policyMode,
                    scenario,
                    authorizationHeader,
                    errorRateTracker.currentErrorRate()
            );
            lastDecision = result.decision();

            PaymentChargeResponse payment = result.body();
            order.setPaymentId(payment.paymentId());
            order.setStatus("PAID");
            orderRepository.save(order);

            errorRateTracker.record(true);
            demoStatsService.recordSuccess(policyMode, lastDecision);
            return toResponse(order);
        } catch (ResponseStatusException ex) {
            errorRateTracker.record(false);
            demoStatsService.recordFailure(classifyOrderSideFailure(scenario, ex), policyMode, lastDecision);
            throw ex;
        } catch (com.aris.common.aris.ArisCallException ex) {
            errorRateTracker.record(false);
            lastDecision = ex.getDecision();
            demoStatsService.recordFailure(classifyOutboundFailure(scenario), policyMode, lastDecision);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment call failed: " + ex.getMessage(), ex);
        } catch (RestClientResponseException ex) {
            errorRateTracker.record(false);
            demoStatsService.recordFailure("PAYMENT", policyMode, lastDecision);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment call failed: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            errorRateTracker.record(false);
            demoStatsService.recordFailure(classifyOutboundFailure(scenario), policyMode, lastDecision);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment call failed: " + ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id, DemoScenario scenario) {
        scenarioBehaviour.assertDbAvailable(scenario);
        return toResponse(orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listByUser(UUID userId, DemoScenario scenario) {
        scenarioBehaviour.assertDbAvailable(scenario);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderService::toResponse)
                .toList();
    }

    private static String classifyOrderSideFailure(DemoScenario scenario, ResponseStatusException ex) {
        if (scenario == DemoScenario.ORDER_DOWN
                || scenario == DemoScenario.ORDER_DB_DOWN
                || scenario == DemoScenario.ORDER_SLOW) {
            return "ORDER";
        }
        String reason = ex.getReason() != null ? ex.getReason().toLowerCase() : "";
        if (reason.contains("order")) {
            return "ORDER";
        }
        if (reason.contains("payment") || reason.contains("partner")) {
            return "PAYMENT";
        }
        return "ORDER";
    }

    private static String classifyOutboundFailure(DemoScenario scenario) {
        if (scenario == DemoScenario.ORDER_DOWN
                || scenario == DemoScenario.ORDER_DB_DOWN
                || scenario == DemoScenario.ORDER_SLOW) {
            return "ORDER";
        }
        if (scenario == DemoScenario.PAYMENT_DOWN
                || scenario == DemoScenario.PAYMENT_SLOW
                || scenario == DemoScenario.PARTNER_TIMEOUT
                || scenario == DemoScenario.BUSY_SPIKE) {
            return "PAYMENT";
        }
        return "PAYMENT";
    }

    private static OrderResponse toResponse(OrderEntity order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getItemName(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getPaymentId(),
                order.getCreatedAt()
        );
    }
}
