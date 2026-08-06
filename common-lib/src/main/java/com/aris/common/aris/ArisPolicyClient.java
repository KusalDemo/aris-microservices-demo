package com.aris.common.aris;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin HTTP client for the external ARIS policy service ({@code POST /decide}).
 * Fail-open: on any transport/HTTP error, returns configured static defaults.
 */
public class ArisPolicyClient {

    private static final Logger log = LoggerFactory.getLogger(ArisPolicyClient.class);

    private final ArisProperties properties;
    private final RestClient restClient;

    public ArisPolicyClient(ArisProperties properties) {
        this(properties, buildRestClient(properties));
    }

    public ArisPolicyClient(ArisProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    private static RestClient buildRestClient(ArisProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        return RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Call ARIS {@code /decide}. Never throws for policy unavailability — fail-open instead.
     */
    public ArisDecideResponse decide(ArisDecideRequest request) {
        try {
            ArisDecideResponse response = restClient.post()
                    .uri(properties.decideUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ArisDecideResponse.class);
            if (response == null || response.retry() == null) {
                log.warn("ARIS /decide returned empty body; fail-open to static defaults");
                return properties.toFailOpenResponse();
            }
            return response;
        } catch (RestClientException ex) {
            log.warn("ARIS policy service unreachable or error (fail-open): {}", ex.getMessage());
            return properties.toFailOpenResponse();
        } catch (RuntimeException ex) {
            log.warn("ARIS policy client unexpected error (fail-open): {}", ex.getMessage());
            return properties.toFailOpenResponse();
        }
    }

    /**
     * Convenience: build request from route / error rate / optional trace, using static suggested triple.
     */
    public ArisDecideResponse decide(String route, double errorRate, String traceId) {
        ArisDecideRequest request = new ArisDecideRequest(
                route,
                errorRate,
                properties.toSuggestedAction(),
                traceId
        );
        return decide(request);
    }

    public ArisProperties getProperties() {
        return properties;
    }
}
