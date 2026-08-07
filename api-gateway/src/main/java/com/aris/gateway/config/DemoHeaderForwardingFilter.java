package com.aris.gateway.config;

import com.aris.common.demo.DemoHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Ensures {@code X-Demo-Policy} and {@code X-Demo-Scenario} reach downstream services.
 */
@Component
public class DemoHeaderForwardingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DemoHeaderForwardingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String policy = request.getHeaders().getFirst(DemoHeaders.POLICY);
        String scenario = request.getHeaders().getFirst(DemoHeaders.SCENARIO);

        boolean hasPolicy = policy != null && !policy.isBlank();
        boolean hasScenario = scenario != null && !scenario.isBlank();

        // Frontend already sets both
        if (hasPolicy && hasScenario) {
            if (log.isDebugEnabled()) {
                log.debug("Passing through demo headers policy={} scenario={} path={}",
                        policy.trim(), scenario.trim(), request.getURI().getPath());
            }
            return chain.filter(exchange);
        }

        // Missing one or both: rebuild headers on a decorator (writable copy).
        final String policyValue = hasPolicy ? policy.trim() : null;
        final String scenarioValue = hasScenario ? scenario.trim() : null;

        HttpHeaders writable = new HttpHeaders();
        writable.putAll(request.getHeaders());
        if (policyValue != null) {
            writable.set(DemoHeaders.POLICY, policyValue);
        }
        if (scenarioValue != null) {
            writable.set(DemoHeaders.SCENARIO, scenarioValue);
        }
        HttpHeaders frozen = HttpHeaders.readOnlyHttpHeaders(writable);

        ServerHttpRequest decorated = new ServerHttpRequestDecorator(request) {
            @Override
            public HttpHeaders getHeaders() {
                return frozen;
            }
        };

        if (log.isDebugEnabled()) {
            log.debug("Decorated demo headers policy={} scenario={} path={}",
                    policyValue, scenarioValue, request.getURI().getPath());
        }

        return chain.filter(exchange.mutate().request(decorated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
