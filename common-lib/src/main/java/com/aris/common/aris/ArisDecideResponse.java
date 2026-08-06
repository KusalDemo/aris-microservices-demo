package com.aris.common.aris;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response body from ARIS policy {@code POST /decide}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArisDecideResponse(
        Integer retry,
        @JsonProperty("backoff_multiplier") Double backoffMultiplier,
        @JsonProperty("timeout_ms") Double timeoutMs,
        @JsonProperty("override_reasons") List<String> overrideReasons,
        @JsonProperty("frozen_active") Boolean frozenActive
) {
    public ArisDecideResponse {
        if (overrideReasons == null) {
            overrideReasons = List.of();
        }
        if (frozenActive == null) {
            frozenActive = Boolean.FALSE;
        }
    }
}
