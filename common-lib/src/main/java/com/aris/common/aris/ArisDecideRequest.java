package com.aris.common.aris;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for ARIS policy {@code POST /decide}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ArisDecideRequest(
        @NotBlank String route,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") @JsonProperty("error_rate") Double errorRate,
        @Valid ArisSuggestedAction suggested,
        @JsonProperty("trace_id") String traceId
) {
}
