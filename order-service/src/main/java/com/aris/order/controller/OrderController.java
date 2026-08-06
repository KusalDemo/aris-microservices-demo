package com.aris.order.controller;

import com.aris.common.demo.DemoHeaders;
import com.aris.common.demo.DemoPolicyMode;
import com.aris.common.demo.DemoScenario;
import com.aris.common.security.ArisJwtService;
import com.aris.order.dto.OrderResponse;
import com.aris.order.dto.PlaceOrderRequest;
import com.aris.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = DemoHeaders.POLICY, required = false) String policyHeader,
            @RequestHeader(value = DemoHeaders.SCENARIO, required = false) String scenarioHeader,
            HttpServletRequest httpRequest
    ) {
        DemoPolicyMode policyMode = parsePolicy(policyHeader);
        DemoScenario scenario = parseScenario(scenarioHeader);
        String authorization = httpRequest.getHeader("Authorization");
        return orderService.placeOrder(extractUserId(jwt), request, policyMode, scenario, authorization);
    }

    @GetMapping("/{id}")
    public OrderResponse getById(
            @PathVariable UUID id,
            @RequestHeader(value = DemoHeaders.SCENARIO, required = false) String scenarioHeader
    ) {
        return orderService.getById(id, parseScenario(scenarioHeader));
    }

    @GetMapping
    public List<OrderResponse> list(
            @RequestParam UUID userId,
            @RequestHeader(value = DemoHeaders.SCENARIO, required = false) String scenarioHeader
    ) {
        return orderService.listByUser(userId, parseScenario(scenarioHeader));
    }

    private static UUID extractUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString(ArisJwtService.CLAIM_USER_ID);
        if (userId == null || userId.isBlank()) {
            userId = jwt.getSubject();
        }
        return UUID.fromString(userId);
    }

    private static DemoPolicyMode parsePolicy(String raw) {
        try {
            return DemoPolicyMode.fromHeader(raw);
        } catch (IllegalArgumentException ex) {
            return DemoPolicyMode.STATIC;
        }
    }

    private static DemoScenario parseScenario(String raw) {
        try {
            return DemoScenario.fromHeader(raw);
        } catch (IllegalArgumentException ex) {
            return DemoScenario.NORMAL;
        }
    }
}
