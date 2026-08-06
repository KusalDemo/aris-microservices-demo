package com.aris.common.demo;

import java.util.Locale;

/**
 * How outbound retries are chosen for a request.
 */
public enum DemoPolicyMode {
    STATIC,
    ARIS;

    public static DemoPolicyMode fromHeader(String raw) {
        if (raw == null || raw.isBlank()) {
            return STATIC;
        }
        return DemoPolicyMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
