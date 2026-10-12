package dev.ivanjimenez.connector.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Registro de capturas, reembolsos y cancelaciones.
 *
 * VTEX puede reintentar la misma operación (mismo requestId) si no recibió respuesta a tiempo.
 * La restricción única evita que un reintento capture o reembolse dos veces.
 */
@Entity
@Table(name = "payment_operations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"paymentId", "type", "requestId"}))
public class PaymentOperation {

    public enum Type { SETTLEMENT, REFUND, CANCELLATION }

    @Id
    private String id;

    @Column(nullable = false)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String requestId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant createdAt;

    protected PaymentOperation() {
        // JPA
    }

    public PaymentOperation(String id, String paymentId, Type type, String requestId, BigDecimal amount, Instant createdAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.type = type;
        this.requestId = requestId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getPaymentId() { return paymentId; }
    public Type getType() { return type; }
    public String getRequestId() { return requestId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
