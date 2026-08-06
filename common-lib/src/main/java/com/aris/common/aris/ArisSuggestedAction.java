package com.aris.common.aris;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Suggested retry / backoff / timeout triple sent to ARIS {@code /decide}.
 */
public record ArisSuggestedAction(
        @NotNull @Min(0) Integer retry,
        @NotNull @DecimalMin("0.0") @JsonProperty("backoff_multiplier") Double backoffMultiplier,
        @NotNull @DecimalMin("0.0") @JsonProperty("timeout_ms") Double timeoutMs
) {
}
