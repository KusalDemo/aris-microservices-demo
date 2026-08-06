package com.aris.order.client;

import com.aris.common.aris.ArisAwareRestClient;
import com.aris.common.aris.ArisHttpResult;
import com.aris.common.demo.DemoHeaders;
import com.aris.common.demo.DemoPolicyMode;
import com.aris.common.demo.DemoScenario;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentClient {

    public static final String ROUTE = "order-service.chargePayment";

    private final ArisAwareRestClient arisAwareRestClient;
    private final String chargeUrl;

    public PaymentClient(
            ArisAwareRestClient arisAwareRestClient,
            @Value("${aris.payment.charge-url:http://payment-service/api/payments/charge}") String chargeUrl
    ) {
        this.arisAwareRestClient = arisAwareRestClient;
        this.chargeUrl = chargeUrl;
    }

    public ArisHttpResult<PaymentChargeResponse> charge(
            PaymentChargeRequest request,
            DemoPolicyMode policyMode,
            DemoScenario scenario,
            String authorizationHeader,
            double errorRate
    ) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.put("Authorization", authorizationHeader);
        }
        headers.put(DemoHeaders.POLICY, policyMode.name());
        headers.put(DemoHeaders.SCENARIO, scenario.name());
        headers.put("X-Trace-Id", java.util.UUID.randomUUID().toString());

        return arisAwareRestClient.postResult(
                chargeUrl,
                request,
                PaymentChargeResponse.class,
                ROUTE,
                policyMode,
                headers,
                errorRate
        );
    }
}
