package dev.ivanjimenez.connector.web.dto;

import java.math.BigDecimal;

/** Si la captura falla, settleId va en null: así es como VTEX distingue un fallo. */
public record SettlementResponse(
        String paymentId,
        String settleId,
        BigDecimal value,
        String code,
        String message,
        String requestId) {
}
