package dev.ivanjimenez.connector.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CancellationRequest(
        @NotBlank String requestId,
        String authorizationId) {
}
