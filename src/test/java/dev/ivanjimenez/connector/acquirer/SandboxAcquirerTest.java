package dev.ivanjimenez.connector.acquirer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import dev.ivanjimenez.connector.domain.PaymentStatus;

import org.junit.jupiter.api.Test;

class SandboxAcquirerTest {

    private final SandboxAcquirer acquirer = new SandboxAcquirer();
    private final BigDecimal amount = new BigDecimal("1499.90");

    @Test
    void approvesTheApprovedTestCard() {
        AuthorizationResult result = acquirer.authorize(SandboxAcquirer.CARD_APPROVED, amount, "MXN");

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.authorizationId()).startsWith("AUT-");
        assertThat(result.nsu()).hasSize(6);
    }

    @Test
    void deniesTheDeniedTestCard() {
        AuthorizationResult result = acquirer.authorize(SandboxAcquirer.CARD_DENIED, amount, "MXN");

        assertThat(result.status()).isEqualTo(PaymentStatus.DENIED);
        assertThat(result.authorizationId()).isNull();
    }

    @Test
    void asyncCardsStayUndefinedButKnowTheirOutcome() {
        AuthorizationResult approvedLater = acquirer.authorize(SandboxAcquirer.CARD_ASYNC_APPROVED, amount, "MXN");
        AuthorizationResult deniedLater = acquirer.authorize(SandboxAcquirer.CARD_ASYNC_DENIED, amount, "MXN");

        assertThat(approvedLater.status()).isEqualTo(PaymentStatus.UNDEFINED);
        assertThat(approvedLater.pendingOutcome()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(deniedLater.status()).isEqualTo(PaymentStatus.UNDEFINED);
        assertThat(deniedLater.pendingOutcome()).isEqualTo(PaymentStatus.DENIED);
    }

    @Test
    void acceptsCardNumbersWithSpaces() {
        AuthorizationResult result = acquirer.authorize("4444 3333 2222 1111", amount, "MXN");

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void deniesCardsThatFailLuhn() {
        AuthorizationResult result = acquirer.authorize("4111111111111112", amount, "MXN");

        assertThat(result.status()).isEqualTo(PaymentStatus.DENIED);
        assertThat(result.code()).isEqualTo("14");
    }

    @Test
    void deniesAmountsOverTheLimitWithInsufficientFunds() {
        AuthorizationResult result = acquirer.authorize("4111111111111111", new BigDecimal("50000.01"), "MXN");

        assertThat(result.status()).isEqualTo(PaymentStatus.DENIED);
        assertThat(result.code()).isEqualTo("51");
    }

    @Test
    void luhnWorksWithKnownNumbers() {
        assertThat(SandboxAcquirer.passesLuhn("4111111111111111")).isTrue();
        assertThat(SandboxAcquirer.passesLuhn("5555555555554444")).isTrue();
        assertThat(SandboxAcquirer.passesLuhn("1234567812345678")).isFalse();
        assertThat(SandboxAcquirer.passesLuhn("abc")).isFalse();
    }
}
