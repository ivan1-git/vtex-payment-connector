package dev.ivanjimenez.connector.service;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String paymentId) {
        super("No existe el pago " + paymentId);
    }
}
