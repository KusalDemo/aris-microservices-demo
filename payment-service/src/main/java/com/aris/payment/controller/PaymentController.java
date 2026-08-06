package com.aris.payment.controller;

import com.aris.common.demo.DemoHeaders;
import com.aris.common.demo.DemoScenario;
import com.aris.payment.dto.ChargeRequest;
import com.aris.payment.dto.ChargeResponse;
import com.aris.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/charge")
    public ChargeResponse charge(
            @Valid @RequestBody ChargeRequest request,
            @RequestHeader(value = DemoHeaders.SCENARIO, required = false) String scenarioHeader
    ) {
        DemoScenario scenario;
        try {
            scenario = DemoScenario.fromHeader(scenarioHeader);
        } catch (IllegalArgumentException ex) {
            scenario = DemoScenario.NORMAL;
        }
        return paymentService.charge(request, scenario);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return paymentService.health();
    }
}
