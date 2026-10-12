package dev.ivanjimenez.connector.web.dto;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Solo mapeo los campos que el conector usa. VTEX manda muchos más (miniCart, comprador,
 * dirección...) y Jackson ignora el resto sin quejarse.
 */
public record CreatePaymentRequest(
        @NotBlank String paymentId,
        @NotBlank String transactionId,
        String orderId,
        String reference,
        @NotBlank String paymentMethod,
        @NotNull @Positive BigDecimal value,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Min(1) Integer installments,
        @Valid CardData card,
        String callbackUrl,
        Boolean sandboxMode) {

    public record CardData(
            String holder,
            @NotBlank String number,
            String csc,
            Expiration expiration) {
    }

    public record Expiration(String month, String year) {
    }

    public int installmentsOrDefault() {
        return installments == null ? 1 : installments;
    }
}
