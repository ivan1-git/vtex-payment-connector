package dev.ivanjimenez.connector.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    /** Pagos asíncronos que el banco ya debería haber resuelto. */
    List<Payment> findByStatusAndCanceledAtIsNullAndResolveAtLessThanEqual(PaymentStatus status, Instant now);

    List<Payment> findByCallbackPendingTrue();
}
