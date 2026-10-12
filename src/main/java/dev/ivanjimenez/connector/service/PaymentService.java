package dev.ivanjimenez.connector.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import dev.ivanjimenez.connector.acquirer.Acquirer;
import dev.ivanjimenez.connector.acquirer.AuthorizationResult;
import dev.ivanjimenez.connector.config.ConnectorProperties;
import dev.ivanjimenez.connector.domain.OperationRejectedException;
import dev.ivanjimenez.connector.domain.Payment;
import dev.ivanjimenez.connector.domain.PaymentOperation;
import dev.ivanjimenez.connector.domain.PaymentOperationRepository;
import dev.ivanjimenez.connector.domain.PaymentRepository;
import dev.ivanjimenez.connector.domain.PaymentStatus;
import dev.ivanjimenez.connector.web.dto.CreatePaymentRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public static final Set<String> SUPPORTED_METHODS = Set.of("Visa", "Mastercard", "American Express");

    private final PaymentRepository payments;
    private final PaymentOperationRepository operations;
    private final Acquirer acquirer;
    private final ConnectorProperties properties;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, PaymentOperationRepository operations, Acquirer acquirer,
                          ConnectorProperties properties, Clock clock) {
        this.payments = payments;
        this.operations = operations;
        this.acquirer = acquirer;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Payment create(CreatePaymentRequest request) {
        // Si VTEX no recibió la respuesta a tiempo, vuelve a mandar el mismo paymentId.
        // Hay que contestar exactamente lo mismo, no volver a cobrar.
        Optional<Payment> existing = payments.findById(request.paymentId());
        if (existing.isPresent()) {
            log.info("Pago {} ya existía, regreso la respuesta guardada", request.paymentId());
            return existing.get();
        }

        String cardNumber = request.card() == null ? null : request.card().number();
        Instant now = Instant.now(clock);

        Payment payment = new Payment(request.paymentId(), request.transactionId(), request.orderId(),
                request.reference(), request.paymentMethod(), request.value(), request.currency(),
                request.installmentsOrDefault(), lastFour(cardNumber), request.callbackUrl(), now);

        AuthorizationResult result = authorize(request.paymentMethod(), cardNumber, request);
        Instant resolveAt = result.isPending() ? now.plus(properties.asyncDelay()) : null;

        payment.applyAuthorization(result.status(), result.pendingOutcome(), resolveAt,
                result.authorizationId(), result.tid(), result.nsu(), result.code(), result.message());
        payments.save(payment);

        log.info("Pago {} por {} {} con tarjeta ****{}: {}", payment.getPaymentId(), payment.getAmount(),
                payment.getCurrency(), payment.getCardLast4(), payment.getStatus());
        return payment;
    }

    @Transactional
    public OperationResult settle(String paymentId, String requestId, BigDecimal value) {
        return runOperation(paymentId, PaymentOperation.Type.SETTLEMENT, requestId, value,
                payment -> payment.settle(value), "Captura realizada");
    }

    @Transactional
    public OperationResult refund(String paymentId, String requestId, BigDecimal value) {
        return runOperation(paymentId, PaymentOperation.Type.REFUND, requestId, value,
                payment -> payment.refund(value), "Reembolso realizado");
    }

    @Transactional
    public OperationResult cancel(String paymentId, String requestId) {
        return runOperation(paymentId, PaymentOperation.Type.CANCELLATION, requestId, null,
                payment -> payment.cancel(Instant.now(clock)), "Pago cancelado");
    }

    /** El "banco" contesta los pagos asíncronos cuyo tiempo de espera ya pasó. */
    @Transactional
    public List<Payment> resolveDuePayments() {
        List<Payment> due = payments.findByStatusAndCanceledAtIsNullAndResolveAtLessThanEqual(
                PaymentStatus.UNDEFINED, Instant.now(clock));
        due.forEach(Payment::resolvePending);
        return due;
    }

    @Transactional(readOnly = true)
    public List<Payment> paymentsAwaitingCallback() {
        return payments.findByCallbackPendingTrue();
    }

    @Transactional
    public void registerCallbackAttempt(String paymentId, boolean delivered) {
        Payment payment = find(paymentId);
        payment.registerCallbackAttempt(delivered, properties.callbackMaxAttempts());
        if (!delivered && !payment.isCallbackPending()) {
            log.error("No pude avisar a VTEX del pago {} después de {} intentos", paymentId, payment.getCallbackAttempts());
        }
    }

    @Transactional(readOnly = true)
    public Payment find(String paymentId) {
        return payments.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private AuthorizationResult authorize(String paymentMethod, String cardNumber, CreatePaymentRequest request) {
        if (!SUPPORTED_METHODS.contains(paymentMethod)) {
            return new AuthorizationResult(PaymentStatus.DENIED, null, null, null, null,
                    "unsupported-method", "Este conector no procesa " + paymentMethod);
        }
        if (cardNumber == null || cardNumber.isBlank()) {
            return new AuthorizationResult(PaymentStatus.DENIED, null, null, null, null,
                    "missing-card", "El pago no trae datos de tarjeta");
        }
        return acquirer.authorize(cardNumber, request.value(), request.currency());
    }

    private OperationResult runOperation(String paymentId, PaymentOperation.Type type, String requestId,
                                         BigDecimal value, Consumer<Payment> action, String okMessage) {
        Payment payment = find(paymentId);

        // Mismo requestId = reintento de VTEX. Regreso el resultado anterior sin tocar montos.
        Optional<PaymentOperation> previous = operations.findByPaymentIdAndTypeAndRequestId(paymentId, type, requestId);
        if (previous.isPresent()) {
            return OperationResult.ok(previous.get().getId(), previous.get().getAmount(), okMessage);
        }

        try {
            action.accept(payment);
        } catch (OperationRejectedException e) {
            log.warn("{} rechazada para el pago {}: {}", type, paymentId, e.getMessage());
            return OperationResult.rejected(e.getCode(), e.getMessage());
        }

        PaymentOperation operation = new PaymentOperation(prefix(type) + UUID.randomUUID(), paymentId, type,
                requestId, value, Instant.now(clock));
        operations.save(operation);
        log.info("{} {} aplicada al pago {} por {}", type, operation.getId(), paymentId, value);
        return OperationResult.ok(operation.getId(), value, okMessage);
    }

    private static String prefix(PaymentOperation.Type type) {
        return switch (type) {
            case SETTLEMENT -> "STL-";
            case REFUND -> "REF-";
            case CANCELLATION -> "CAN-";
        };
    }

    private static String lastFour(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }
        String digits = cardNumber.replaceAll("\\D", "");
        return digits.length() < 4 ? null : digits.substring(digits.length() - 4);
    }
}
