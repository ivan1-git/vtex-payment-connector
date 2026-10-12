package dev.ivanjimenez.connector.service;

import java.math.BigDecimal;

/**
 * Resultado de capturar, reembolsar o cancelar.
 * Si operationId es null, la operación fue rechazada y code/message dicen por qué.
 */
public record OperationResult(String operationId, BigDecimal value, String code, String message) {

    public static OperationResult ok(String operationId, BigDecimal value, String message) {
        return new OperationResult(operationId, value, "00", message);
    }

    public static OperationResult rejected(String code, String message) {
        return new OperationResult(null, BigDecimal.ZERO, code, message);
    }

    public boolean succeeded() {
        return operationId != null;
    }
}
