package com.aris.common.aris;

/**
 * Thrown when an ARIS-aware outbound call fails after applying the resolved decision.
 */
public class ArisCallException extends RuntimeException {

    private final ArisDecideResponse decision;
    private final int retryAttempts;

    public ArisCallException(String message, Throwable cause, ArisDecideResponse decision, int retryAttempts) {
        super(message, cause);
        this.decision = decision;
        this.retryAttempts = retryAttempts;
    }

    public ArisDecideResponse getDecision() {
        return decision;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }
}
