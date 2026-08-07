package com.aris.order.dto;

import java.util.Map;

public record DemoStatsResponse(
        long totalRequests,
        long successCount,
        long failCount,
        long retryAttemptsTotal,
        long estimatedExtraCalls,
        String lastPolicyMode,
        String lastScenario,
        String lastFailureLocation,
        Map<String, Object> lastArisDecision,
        int lastRetriesObserved
) {
}
