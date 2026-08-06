package com.aris.order.config;

import com.aris.common.aris.ArisAwareRestClient;
import com.aris.common.aris.ArisPolicyClient;
import com.aris.common.aris.ArisProperties;
import com.aris.common.aris.ArisRetryListener;
import com.aris.common.security.JwtDecoders;
import com.aris.common.security.JwtProperties;
import com.aris.order.service.DemoStatsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, ArisProperties.class})
public class OrderConfig {

    @Bean
    JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return JwtDecoders.servletDecoder(jwtProperties);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    ArisPolicyClient arisPolicyClient(ArisProperties arisProperties) {
        return new ArisPolicyClient(arisProperties);
    }

    @Bean
    ArisRetryListener arisRetryListener(DemoStatsService demoStatsService) {
        return (route, attempt, lastThrowable) -> demoStatsService.recordRetryAttempt();
    }

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // High ceiling; per-attempt timeout is enforced inside ArisAwareRestClient.
        factory.setConnectTimeout(2_000);
        factory.setReadTimeout(30_000);
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    ArisAwareRestClient arisAwareRestClient(
            RestClient.Builder loadBalancedRestClientBuilder,
            ArisPolicyClient arisPolicyClient,
            ArisProperties arisProperties,
            ArisRetryListener arisRetryListener
    ) {
        return new ArisAwareRestClient(
                loadBalancedRestClientBuilder.build(),
                arisPolicyClient,
                arisProperties,
                arisRetryListener
        );
    }
}
