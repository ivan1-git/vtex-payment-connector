package dev.ivanjimenez.connector.web;

import dev.ivanjimenez.connector.acquirer.Acquirer;
import dev.ivanjimenez.connector.config.ConnectorProperties;
import dev.ivanjimenez.connector.domain.Payment;
import dev.ivanjimenez.connector.web.dto.PaymentResponse;

import org.springframework.stereotype.Component;

/**
 * Arma la respuesta que VTEX espera a partir de un pago guardado.
 * La usan tanto el endpoint de creación como el callback de pagos asíncronos.
 */
@Component
public class PaymentResponseMapper {

    // 6 horas para capturar automáticamente y 30 minutos si pasó por antifraude
    private static final long DELAY_TO_AUTO_SETTLE = 21_600;
    private static final long DELAY_TO_AUTO_SETTLE_AFTER_ANTIFRAUD = 1_800;

    private final Acquirer acquirer;
    private final ConnectorProperties properties;

    public PaymentResponseMapper(Acquirer acquirer, ConnectorProperties properties) {
        this.acquirer = acquirer;
        this.properties = properties;
    }

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getStatus().toVtex(),
                payment.getAuthorizationId(),
                payment.getTid(),
                payment.getNsu(),
                acquirer.name(),
                payment.getCode(),
                payment.getMessage(),
                DELAY_TO_AUTO_SETTLE,
                DELAY_TO_AUTO_SETTLE_AFTER_ANTIFRAUD,
                properties.delayToCancel());
    }
}
