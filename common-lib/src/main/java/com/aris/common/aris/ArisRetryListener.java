package com.aris.common.aris;

/**
 * Optional callback so callers (e.g. demo stats) can observe retries without owning Resilience4j.
 */
@FunctionalInterface
public interface ArisRetryListener {

    void onRetry(String route, int attempt, Throwable lastThrowable);

    ArisRetryListener NOOP = (route, attempt, lastThrowable) -> {
    };
}
