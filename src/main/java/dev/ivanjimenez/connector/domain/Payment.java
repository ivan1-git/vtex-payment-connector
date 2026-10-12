package dev.ivanjimenez.connector.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un pago tal como lo ve el conector.
 *
 * Las reglas (cuánto se puede capturar, cuándo se puede cancelar) están aquí y no en el servicio,
 * para que no se puedan saltar por accidente desde otro lado.
 *
 * Nunca guardamos el número de tarjeta completo, solo los últimos 4 dígitos.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private String paymentId;

    @Column(nullable = false)
    private String transactionId;

    private String orderId;
    private String reference;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private int installments;

    @Column(length = 4)
    private String cardLast4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // Para pagos asíncronos: lo que el "banco" va a decidir y cuándo
    @Enumerated(EnumType.STRING)
    private PaymentStatus pendingOutcome;
    private Instant resolveAt;

    private String authorizationId;
    private String tid;
    private String nsu;
    private String code;
    private String message;

    private String callbackUrl;
    private boolean callbackPending;
    private int callbackAttempts;
    private boolean callbackDelivered;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    private Instant canceledAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected Payment() {
        // JPA
    }

    public Payment(String paymentId, String transactionId, String orderId, String reference, String paymentMethod,
                   BigDecimal amount, String currency, int installments, String cardLast4, String callbackUrl,
                   Instant createdAt) {
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.reference = reference;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
        this.installments = installments;
        this.cardLast4 = cardLast4;
        this.callbackUrl = callbackUrl;
        this.createdAt = createdAt;
        this.status = PaymentStatus.UNDEFINED;
    }

    public void applyAuthorization(PaymentStatus status, PaymentStatus pendingOutcome, Instant resolveAt,
                                   String authorizationId, String tid, String nsu, String code, String message) {
        this.status = status;
        this.pendingOutcome = pendingOutcome;
        this.resolveAt = resolveAt;
        this.authorizationId = authorizationId;
        this.tid = tid;
        this.nsu = nsu;
        this.code = code;
        this.message = message;
    }

    /** El banco respondió por fin un pago que estaba en UNDEFINED. */
    public void resolvePending() {
        if (status != PaymentStatus.UNDEFINED || pendingOutcome == null) {
            return;
        }
        this.status = pendingOutcome;
        this.message = pendingOutcome == PaymentStatus.APPROVED ? "Pago aprobado por el banco" : "Pago rechazado por el banco";
        this.code = pendingOutcome == PaymentStatus.APPROVED ? "00" : "51";
        this.pendingOutcome = null;
        this.resolveAt = null;
        // VTEX se enteró del UNDEFINED, ahora hay que avisarle del resultado final
        this.callbackPending = callbackUrl != null;
    }

    public void settle(BigDecimal value) {
        if (isCanceled()) {
            throw new OperationRejectedException("payment-canceled", "No se puede capturar un pago cancelado");
        }
        if (status != PaymentStatus.APPROVED) {
            throw new OperationRejectedException("payment-not-approved", "Solo se capturan pagos aprobados");
        }
        BigDecimal available = amount.subtract(settledAmount);
        if (value.compareTo(available) > 0) {
            throw new OperationRejectedException("amount-exceeded",
                    "Se intentó capturar " + value + " pero solo quedan " + available + " autorizados");
        }
        settledAmount = settledAmount.add(value);
    }

    public void refund(BigDecimal value) {
        BigDecimal refundable = settledAmount.subtract(refundedAmount);
        if (refundable.signum() == 0) {
            throw new OperationRejectedException("nothing-to-refund", "El pago no tiene montos capturados por reembolsar");
        }
        if (value.compareTo(refundable) > 0) {
            throw new OperationRejectedException("amount-exceeded",
                    "Se intentó reembolsar " + value + " pero solo quedan " + refundable + " capturados");
        }
        refundedAmount = refundedAmount.add(value);
    }

    public void cancel(Instant now) {
        if (isCanceled()) {
            return;
        }
        if (status == PaymentStatus.DENIED) {
            throw new OperationRejectedException("payment-denied", "El pago fue rechazado, no hay nada que cancelar");
        }
        if (settledAmount.signum() > 0) {
            throw new OperationRejectedException("payment-settled", "El pago ya tiene capturas; usa un reembolso");
        }
        canceledAt = now;
        pendingOutcome = null;
        resolveAt = null;
    }

    public boolean isCanceled() {
        return canceledAt != null;
    }

    public void registerCallbackAttempt(boolean delivered, int maxAttempts) {
        callbackAttempts++;
        callbackDelivered = delivered;
        if (delivered || callbackAttempts >= maxAttempts) {
            callbackPending = false;
        }
    }

    public String getPaymentId() { return paymentId; }
    public String getTransactionId() { return transactionId; }
    public String getOrderId() { return orderId; }
    public String getReference() { return reference; }
    public String getPaymentMethod() { return paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public int getInstallments() { return installments; }
    public String getCardLast4() { return cardLast4; }
    public PaymentStatus getStatus() { return status; }
    public PaymentStatus getPendingOutcome() { return pendingOutcome; }
    public Instant getResolveAt() { return resolveAt; }
    public String getAuthorizationId() { return authorizationId; }
    public String getTid() { return tid; }
    public String getNsu() { return nsu; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getCallbackUrl() { return callbackUrl; }
    public boolean isCallbackPending() { return callbackPending; }
    public int getCallbackAttempts() { return callbackAttempts; }
    public boolean isCallbackDelivered() { return callbackDelivered; }
    public BigDecimal getSettledAmount() { return settledAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public Instant getCanceledAt() { return canceledAt; }
    public Instant getCreatedAt() { return createdAt; }
}
