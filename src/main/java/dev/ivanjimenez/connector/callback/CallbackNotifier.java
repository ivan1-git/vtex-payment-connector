package dev.ivanjimenez.connector.callback;

import dev.ivanjimenez.connector.web.dto.PaymentResponse;

/** Avisa a VTEX el resultado final de un pago que quedó en UNDEFINED. */
public interface CallbackNotifier {

    /** @return true si VTEX recibió el aviso */
    boolean notify(String callbackUrl, PaymentResponse payment);
}
