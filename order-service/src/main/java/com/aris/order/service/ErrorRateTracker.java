package com.aris.order.service;

import java.util.ArrayDeque;
import java.util.Deque;
import org.springframework.stereotype.Component;

/**
 * Simple in-process sliding window error rate for ARIS {@code /decide} input.
 */
@Component
public class ErrorRateTracker {

    private static final int WINDOW = 20;
    private final Deque<Boolean> outcomes = new ArrayDeque<>();

    public synchronized void record(boolean success) {
        if (outcomes.size() >= WINDOW) {
            outcomes.removeFirst();
        }
        outcomes.addLast(success);
    }

    public synchronized double currentErrorRate() {
        if (outcomes.isEmpty()) {
            return 0.0;
        }
        long failures = outcomes.stream().filter(ok -> !ok).count();
        return (double) failures / (double) outcomes.size();
    }
}
