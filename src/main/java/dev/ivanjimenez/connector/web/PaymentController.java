package dev.ivanjimenez.connector.web;

import dev.ivanjimenez.connector.service.OperationResult;
import dev.ivanjimenez.connector.service.PaymentService;
import dev.ivanjimenez.connector.web.dto.CancellationRequest;
import dev.ivanjimenez.connector.web.dto.CancellationResponse;
import dev.ivanjimenez.connector.web.dto.CreatePaymentRequest;
import dev.ivanjimenez.connector.web.dto.PaymentResponse;
import dev.ivanjimenez.connector.web.dto.RefundRequest;
import dev.ivanjimenez.connector.web.dto.RefundResponse;
import dev.ivanjimenez.connector.web.dto.SettlementRequest;
import dev.ivanjimenez.connector.web.dto.SettlementResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los endpoints que VTEX llama durante la vida de un pago:
 * autorizar, capturar, reembolsar y cancelar.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;
    private final PaymentResponseMapper mapper;

    public PaymentController(PaymentService service, PaymentResponseMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return mapper.toResponse(service.create(request));
    }

    @PostMapping("/{paymentId}/settlements")
    public ResponseEntity<SettlementResponse> settle(@PathVariable String paymentId,
                                                     @Valid @RequestBody SettlementRequest request) {
        OperationResult result = service.settle(paymentId, request.requestId(), request.value());
        return withStatus(result, new SettlementResponse(paymentId, result.operationId(), result.value(),
                result.code(), result.message(), request.requestId()));
    }

    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<RefundResponse> refund(@PathVariable String paymentId,
                                                 @Valid @RequestBody RefundRequest request) {
        OperationResult result = service.refund(paymentId, request.requestId(), request.value());
        return withStatus(result, new RefundResponse(paymentId, result.operationId(), result.value(),
                result.code(), result.message(), request.requestId()));
    }

    @PostMapping("/{paymentId}/cancellations")
    public ResponseEntity<CancellationResponse> cancel(@PathVariable String paymentId,
                                                       @Valid @RequestBody CancellationRequest request) {
        OperationResult result = service.cancel(paymentId, request.requestId());
        return withStatus(result, new CancellationResponse(paymentId, result.operationId(),
                result.code(), result.message(), request.requestId()));
    }

    private static <T> ResponseEntity<T> withStatus(OperationResult result, T body) {
        HttpStatus status = result.succeeded() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }
}
