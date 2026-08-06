package com.aris.gateway.config;

import com.aris.common.demo.DemoHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Re-asserts {@code X-Demo-Policy} and {@code X-Demo-Scenario} so downstream services always receive them.
 */
@Component
public class DemoHeaderForwardingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DemoHeaderForwardingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String policy = request.getHeaders().getFirst(DemoHeaders.POLICY);
        String scenario = request.getHeaders().getFirst(DemoHeaders.SCENARIO);

        ServerHttpRequest.Builder mutated = request.mutate();
        if (policy != null && !policy.isBlank()) {
            mutated.header(DemoHeaders.POLICY, policy.trim());
        }
        if (scenario != null && !scenario.isBlank()) {
            mutated.header(DemoHeaders.SCENARIO, scenario.trim());
        }

        if (log.isDebugEnabled()) {
            log.debug("Forwarding demo headers policy={} scenario={} path={}",
                    policy, scenario, request.getURI().getPath());
        }

        return chain.filter(exchange.mutate().request(mutated.build()).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
