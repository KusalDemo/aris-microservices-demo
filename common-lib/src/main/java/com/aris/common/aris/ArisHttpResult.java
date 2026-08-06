package com.aris.common.aris;

/**
 * Result of an ARIS-aware outbound HTTP call (body + policy decision + attempt counts).
 */
public record ArisHttpResult<T>(
        T body,
        ArisDecideResponse decision,
        int retryAttempts,
        int totalAttempts
) {
}
