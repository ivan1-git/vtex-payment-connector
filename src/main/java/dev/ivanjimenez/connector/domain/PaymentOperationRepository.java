package dev.ivanjimenez.connector.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOperationRepository extends JpaRepository<PaymentOperation, String> {

    Optional<PaymentOperation> findByPaymentIdAndTypeAndRequestId(String paymentId, PaymentOperation.Type type, String requestId);
}
