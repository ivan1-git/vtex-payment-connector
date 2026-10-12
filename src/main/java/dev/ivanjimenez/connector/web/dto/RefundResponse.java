package dev.ivanjimenez.connector.web.dto;

import java.math.BigDecimal;

public record RefundResponse(
        String paymentId,
        String refundId,
        BigDecimal value,
        String code,
        String message,
        String requestId) {
}
