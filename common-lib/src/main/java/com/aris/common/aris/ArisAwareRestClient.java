package com.aris.common.aris;

import com.aris.common.demo.DemoPolicyMode;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Business helper: resolve ARIS or STATIC retry settings, then execute an outbound HTTP call.
 * <p>
 * Rule: {@code maxAttempts = retry + 1} (retry=2 means up to 3 tries).
 */
public class ArisAwareRestClient {

    private static final Logger log = LoggerFactory.getLogger(ArisAwareRestClient.class);

    private final RestClient restClient;
    private final ArisPolicyClient policyClient;
    private final ArisProperties properties;
    private final ArisRetryListener retryListener;

    public ArisAwareRestClient(RestClient restClient, ArisPolicyClient policyClient, ArisProperties properties) {
        this(restClient, policyClient, properties, ArisRetryListener.NOOP);
    }

    public ArisAwareRestClient(
            RestClient restClient,
            ArisPolicyClient policyClient,
            ArisProperties properties,
            ArisRetryListener retryListener
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.policyClient = Objects.requireNonNull(policyClient);
        this.properties = Objects.requireNonNull(properties);
        this.retryListener = retryListener != null ? retryListener : ArisRetryListener.NOOP;
    }

    public <T> T post(
            String url,
            Object body,
            Class<T> responseType,
            String route,
            DemoPolicyMode policyMode,
            Map<String, String> extraHeaders,
            double errorRate
    ) {
        return postResult(url, body, responseType, route, policyMode, extraHeaders, errorRate).body();
    }

    public <T> ArisHttpResult<T> postResult(
            String url,
            Object body,
            Class<T> responseType,
            String route,
            DemoPolicyMode policyMode,
            Map<String, String> extraHeaders,
            double errorRate
    ) {
        ArisDecideResponse decision = resolveDecision(route, policyMode, errorRate, extraHeaders);
        Duration perAttemptTimeout = Duration.ofMillis(Math.max(1L, decision.timeoutMs().longValue()));
        int maxAttempts = Math.max(1, decision.retry() + 1);
        AtomicInteger retries = new AtomicInteger();

        Retry retry = Retry.of(route + "-" + UUID.randomUUID(), RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        Duration.ofMillis(100),
                        decision.backoffMultiplier() == null || decision.backoffMultiplier() <= 0
                                ? 1.0
                                : decision.backoffMultiplier()))
                .retryOnException(ex -> true)
                .build());

        retry.getEventPublisher().onRetry(event -> {
            retries.incrementAndGet();
            retryListener.onRetry(route, event.getNumberOfRetryAttempts(), event.getLastThrowable());
        });

        Supplier<T> supplier = Retry.decorateSupplier(retry, () ->
                executePost(url, body, responseType, extraHeaders, perAttemptTimeout));

        try {
            T responseBody = supplier.get();
            int retryAttempts = retries.get();
            return new ArisHttpResult<>(responseBody, decision, retryAttempts, retryAttempts + 1);
        } catch (RuntimeException ex) {
            log.debug("ARIS-aware POST failed after retries for route={} timeoutMs={}: {}",
                    route, perAttemptTimeout.toMillis(), ex.getMessage());
            throw new ArisCallException(
                    "ARIS-aware outbound call failed for route=" + route + ": " + ex.getMessage(),
                    ex,
                    decision,
                    retries.get()
            );
        }
    }

    public ArisDecideResponse resolveDecision(
            String route,
            DemoPolicyMode policyMode,
            double errorRate,
            Map<String, String> extraHeaders
    ) {
        DemoPolicyMode mode = policyMode != null ? policyMode : DemoPolicyMode.STATIC;
        if (mode == DemoPolicyMode.STATIC) {
            return new ArisDecideResponse(
                    properties.getStaticDefaults().getRetry(),
                    properties.getStaticDefaults().getBackoffMultiplier(),
                    (double) properties.getStaticDefaults().getTimeoutMs(),
                    java.util.List.of(),
                    false
            );
        }
        String traceId = extraHeaders != null ? extraHeaders.get("X-Trace-Id") : null;
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        return policyClient.decide(route, errorRate, traceId);
    }

    private <T> T executePost(
            String url,
            Object body,
            Class<T> responseType,
            Map<String, String> extraHeaders,
            Duration perAttemptTimeout
    ) {
        Objects.requireNonNull(perAttemptTimeout, "perAttemptTimeout");
        return withAttemptTimeout(perAttemptTimeout, () -> restClient.post()
                .uri(url)
                .headers(h -> applyHeaders(h, extraHeaders))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType));
    }

    private static void applyHeaders(HttpHeaders headers, Map<String, String> extraHeaders) {
        if (extraHeaders == null) {
            return;
        }
        Map<String, String> copy = new LinkedHashMap<>(extraHeaders);
        copy.forEach(headers::set);
    }

    private static <T> T withAttemptTimeout(Duration timeout, Callable<T> callable) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "aris-http-attempt");
            t.setDaemon(true);
            return t;
        });
        Future<T> future = executor.submit(callable);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new IllegalStateException("Outbound call exceeded per-attempt timeout of " + timeout.toMillis() + "ms", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new IllegalStateException("Outbound call interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(cause);
        } finally {
            executor.shutdownNow();
        }
    }
}
