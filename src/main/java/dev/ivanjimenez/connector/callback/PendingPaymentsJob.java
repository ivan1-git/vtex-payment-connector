package dev.ivanjimenez.connector.callback;

import dev.ivanjimenez.connector.domain.Payment;
import dev.ivanjimenez.connector.service.PaymentService;
import dev.ivanjimenez.connector.web.PaymentResponseMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada pocos segundos:
 * 1. le "pregunta al banco" por los pagos asíncronos que ya deberían tener respuesta
 * 2. avisa a VTEX por el callbackUrl, con reintentos si VTEX no contesta
 */
@Component
public class PendingPaymentsJob {

    private static final Logger log = LoggerFactory.getLogger(PendingPaymentsJob.class);

    private final PaymentService service;
    private final CallbackNotifier notifier;
    private final PaymentResponseMapper mapper;

    public PendingPaymentsJob(PaymentService service, CallbackNotifier notifier, PaymentResponseMapper mapper) {
        this.service = service;
        this.notifier = notifier;
        this.mapper = mapper;
    }

    @Scheduled(fixedDelayString = "${connector.job-interval-ms:5000}",
               initialDelayString = "${connector.job-interval-ms:5000}")
    public void run() {
        for (Payment payment : service.resolveDuePayments()) {
            log.info("El banco resolvió el pago {}: {}", payment.getPaymentId(), payment.getStatus());
        }

        for (Payment payment : service.paymentsAwaitingCallback()) {
            boolean delivered = notifier.notify(payment.getCallbackUrl(), mapper.toResponse(payment));
            service.registerCallbackAttempt(payment.getPaymentId(), delivered);
        }
    }
}
