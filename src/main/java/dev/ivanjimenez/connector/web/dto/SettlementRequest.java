package dev.ivanjimenez.connector.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SettlementRequest(
        @NotBlank String requestId,
        String transactionId,
        String authorizationId,
        String tid,
        @NotNull @Positive BigDecimal value) {
}
