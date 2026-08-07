package com.aris.order.service;

import com.aris.common.aris.ArisDecideResponse;
import com.aris.common.demo.DemoPolicyMode;
import com.aris.common.demo.DemoScenario;
import com.aris.order.dto.DemoStatsResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class DemoStatsService {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failCount = new AtomicLong();
    private final AtomicLong retryAttemptsTotal = new AtomicLong();
    private final AtomicInteger lastRetriesObserved = new AtomicInteger(0);
    private final ThreadLocal<Integer> retriesThisCall = ThreadLocal.withInitial(() -> 0);
    private final AtomicReference<String> lastPolicyMode = new AtomicReference<>("NONE");
    private final AtomicReference<String> lastScenario = new AtomicReference<>("NONE");
    private final AtomicReference<String> lastFailureLocation = new AtomicReference<>("NONE");
    private final AtomicReference<Map<String, Object>> lastArisDecision = new AtomicReference<>(Map.of());

    public void recordStart(DemoPolicyMode policyMode, DemoScenario scenario) {
        totalRequests.incrementAndGet();
        lastPolicyMode.set(policyMode != null ? policyMode.name() : "NONE");
        lastScenario.set(scenario != null ? scenario.name() : "NONE");
        lastRetriesObserved.set(0);
        retriesThisCall.set(0);
    }

    public void recordRetryAttempt() {
        retryAttemptsTotal.incrementAndGet();
        retriesThisCall.set(retriesThisCall.get() + 1);
    }

    public void finishCallRetries(int retriesFromResilience4j) {
        int expected = Math.max(0, retriesFromResilience4j);
        int counted = Math.max(0, retriesThisCall.get());
        lastRetriesObserved.set(expected);
        int gap = expected - counted;
        if (gap > 0) {
            retryAttemptsTotal.addAndGet(gap);
        }
        retriesThisCall.set(0);
    }

    public void recordSuccess(DemoPolicyMode policyMode, ArisDecideResponse decision) {
        successCount.incrementAndGet();
        lastFailureLocation.set("NONE");
        storeDecision(policyMode, decision);
    }

    public void recordFailure(String location, DemoPolicyMode policyMode, ArisDecideResponse decision) {
        failCount.incrementAndGet();
        lastFailureLocation.set(location != null ? location : "ORDER");
        storeDecision(policyMode, decision);
    }

    public int getLastRetriesObserved() {
        return lastRetriesObserved.get();
    }

    public DemoStatsResponse snapshot() {
        long retries = retryAttemptsTotal.get();
        return new DemoStatsResponse(
                totalRequests.get(),
                successCount.get(),
                failCount.get(),
                retries,
                retries,
                lastPolicyMode.get(),
                lastScenario.get(),
                lastFailureLocation.get(),
                lastArisDecision.get(),
                lastRetriesObserved.get()
        );
    }

    public void reset() {
        totalRequests.set(0);
        successCount.set(0);
        failCount.set(0);
        retryAttemptsTotal.set(0);
        lastRetriesObserved.set(0);
        lastPolicyMode.set("NONE");
        lastScenario.set("NONE");
        lastFailureLocation.set("NONE");
        lastArisDecision.set(Map.of());
    }

    private void storeDecision(DemoPolicyMode policyMode, ArisDecideResponse decision) {
        if (policyMode != DemoPolicyMode.ARIS || decision == null) {
            lastArisDecision.set(Map.of());
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("retry", decision.retry());
        map.put("backoff_multiplier", decision.backoffMultiplier());
        map.put("timeout_ms", decision.timeoutMs());
        map.put("override_reasons", decision.overrideReasons());
        map.put("frozen_active", decision.frozenActive());
        lastArisDecision.set(map);
    }
}
