package com.aris.common.aris;

/**
 * Documents fail-open / STATIC defaults used when ARIS is unreachable or policy mode is STATIC.
 *
 * <pre>
 * retry               = 2
 * backoff_multiplier  = 1.5
 * timeout_ms          = 1000
 * maxAttempts         = retry + 1  (so retry=2 → 3 attempts)
 * </pre>
 */
public final class ArisDefaults {

    public static final int RETRY = 2;
    public static final double BACKOFF_MULTIPLIER = 1.5;
    public static final long TIMEOUT_MS = 1000L;

    private ArisDefaults() {
    }
}
