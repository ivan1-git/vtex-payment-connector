package dev.ivanjimenez.connector.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final Instant NOW = Instant.parse("2026-10-12T15:00:00Z");

    @Test
    void allowsPartialSettlementsUntilTheAuthorizedAmount() {
        Payment payment = approvedPayment("1000.00");

        payment.settle(new BigDecimal("600.00"));
        payment.settle(new BigDecimal("400.00"));

        assertThat(payment.getSettledAmount()).isEqualByComparingTo("1000.00");
        assertThatThrownBy(() -> payment.settle(new BigDecimal("0.01")))
                .isInstanceOf(OperationRejectedException.class)
                .hasMessageContaining("solo quedan 0.00");
    }

    @Test
    void cannotSettleADeniedPayment() {
        Payment payment = newPayment("100.00");
        payment.applyAuthorization(PaymentStatus.DENIED, null, null, null, "TID", null, "05", "Rechazado");

        assertThatThrownBy(() -> payment.settle(new BigDecimal("100.00")))
                .isInstanceOf(OperationRejectedException.class)
                .extracting("code").isEqualTo("payment-not-approved");
    }

    @Test
    void refundsOnlyWhatWasSettled() {
        Payment payment = approvedPayment("500.00");
        payment.settle(new BigDecimal("300.00"));

        payment.refund(new BigDecimal("100.00"));

        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("100.00");
        assertThatThrownBy(() -> payment.refund(new BigDecimal("250.00")))
                .isInstanceOf(OperationRejectedException.class)
                .extracting("code").isEqualTo("amount-exceeded");
    }

    @Test
    void cancelIsIdempotentButNotAllowedAfterSettlement() {
        Payment canceled = approvedPayment("200.00");
        canceled.cancel(NOW);
        canceled.cancel(NOW.plusSeconds(10));

        assertThat(canceled.getCanceledAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> canceled.settle(new BigDecimal("1.00")))
                .extracting("code").isEqualTo("payment-canceled");

        Payment settled = approvedPayment("200.00");
        settled.settle(new BigDecimal("50.00"));
        assertThatThrownBy(() -> settled.cancel(NOW))
                .extracting("code").isEqualTo("payment-settled");
    }

    @Test
    void resolvingAPendingPaymentMarksTheCallbackAsPending() {
        Payment payment = newPayment("100.00");
        payment.applyAuthorization(PaymentStatus.UNDEFINED, PaymentStatus.APPROVED, NOW, "AUT-1", "TID", "123456",
                "PENDING", "En revisión");

        payment.resolvePending();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.isCallbackPending()).isTrue();
    }

    @Test
    void stopsRetryingTheCallbackAfterMaxAttempts() {
        Payment payment = newPayment("100.00");
        payment.applyAuthorization(PaymentStatus.UNDEFINED, PaymentStatus.DENIED, NOW, null, "TID", null, "PENDING", "");
        payment.resolvePending();

        payment.registerCallbackAttempt(false, 2);
        assertThat(payment.isCallbackPending()).isTrue();

        payment.registerCallbackAttempt(false, 2);
        assertThat(payment.isCallbackPending()).isFalse();
        assertThat(payment.isCallbackDelivered()).isFalse();
    }

    private static Payment newPayment(String amount) {
        return new Payment("PAY-1", "TX-1", "ORD-1", "REF-1", "Visa", new BigDecimal(amount), "MXN", 1, "1111",
                "https://example.com/callback", NOW);
    }

    private static Payment approvedPayment(String amount) {
        Payment payment = newPayment(amount);
        payment.applyAuthorization(PaymentStatus.APPROVED, null, null, "AUT-1", "TID", "123456", "00", "Aprobado");
        return payment;
    }
}
