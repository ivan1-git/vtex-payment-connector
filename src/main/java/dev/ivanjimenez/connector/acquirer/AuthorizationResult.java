package dev.ivanjimenez.connector.acquirer;

import dev.ivanjimenez.connector.domain.PaymentStatus;

/**
 * Respuesta del procesador.
 *
 * @param status         lo que le contestamos a VTEX en este momento
 * @param pendingOutcome solo si status es UNDEFINED: lo que el procesador decidirá después
 */
public record AuthorizationResult(
        PaymentStatus status,
        PaymentStatus pendingOutcome,
        String authorizationId,
        String tid,
        String nsu,
        String code,
        String message) {

    public boolean isPending() {
        return status == PaymentStatus.UNDEFINED;
    }
}
