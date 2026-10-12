package dev.ivanjimenez.connector.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        String paymentId,
        String status,
        String authorizationId,
        String tid,
        String nsu,
        String acquirer,
        String code,
        String message,
        Long delayToAutoSettle,
        Long delayToAutoSettleAfterAntifraud,
        Long delayToCancel) {
}
