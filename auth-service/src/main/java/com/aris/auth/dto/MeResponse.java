package com.aris.auth.dto;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String name,
        String role
) {
}
