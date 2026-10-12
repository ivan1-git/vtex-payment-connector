package dev.ivanjimenez.connector.web.dto;

public record CancellationResponse(
        String paymentId,
        String cancellationId,
        String code,
        String message,
        String requestId) {
}
