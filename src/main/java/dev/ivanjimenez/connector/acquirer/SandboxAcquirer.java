package dev.ivanjimenez.connector.acquirer;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HexFormat;

import dev.ivanjimenez.connector.domain.PaymentStatus;

import org.springframework.stereotype.Component;

/**
 * Procesador de mentira con reglas predecibles.
 *
 * Hay una tarjeta por cada flujo que revisa la Test Suite de VTEX al homologar un conector
 * (aprobado, rechazado, asíncrono aprobado y asíncrono rechazado). Cualquier otra tarjeta
 * se valida con Luhn y un límite de monto, como haría un banco muy simplificado.
 */
@Component
public class SandboxAcquirer implements Acquirer {

    public static final String CARD_APPROVED = "4444333322221111";
    public static final String CARD_DENIED = "4444333322221112";
    public static final String CARD_ASYNC_APPROVED = "4222222222222224";
    public static final String CARD_ASYNC_DENIED = "4222222222222225";

    // Arriba de este monto, el banco de mentira dice "fondos insuficientes"
    static final BigDecimal LIMIT = new BigDecimal("50000");

    private final SecureRandom random = new SecureRandom();

    @Override
    public String name() {
        return "SandboxBank";
    }

    @Override
    public AuthorizationResult authorize(String cardNumber, BigDecimal amount, String currency) {
        String card = cardNumber == null ? "" : cardNumber.replaceAll("\\s", "");

        return switch (card) {
            case CARD_APPROVED -> approved();
            case CARD_DENIED -> denied("05", "Transacción rechazada por el banco emisor");
            case CARD_ASYNC_APPROVED -> pending(PaymentStatus.APPROVED);
            case CARD_ASYNC_DENIED -> pending(PaymentStatus.DENIED);
            default -> evaluateGenericCard(card, amount);
        };
    }

    private AuthorizationResult evaluateGenericCard(String card, BigDecimal amount) {
        if (!passesLuhn(card)) {
            return denied("14", "Número de tarjeta inválido");
        }
        if (amount.compareTo(LIMIT) > 0) {
            return denied("51", "Fondos insuficientes");
        }
        return approved();
    }

    private AuthorizationResult approved() {
        return new AuthorizationResult(PaymentStatus.APPROVED, null,
                "AUT-" + hex(4), hex(6), nsu(), "00", "Pago aprobado");
    }

    private AuthorizationResult denied(String code, String message) {
        return new AuthorizationResult(PaymentStatus.DENIED, null, null, hex(6), null, code, message);
    }

    private AuthorizationResult pending(PaymentStatus outcome) {
        return new AuthorizationResult(PaymentStatus.UNDEFINED, outcome,
                "AUT-" + hex(4), hex(6), nsu(), "PENDING", "El banco está validando el pago");
    }

    static boolean passesLuhn(String number) {
        if (!number.matches("\\d{12,19}")) {
            return false;
        }
        int sum = 0;
        boolean doubleIt = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    private String hex(int bytes) {
        byte[] buffer = new byte[bytes];
        random.nextBytes(buffer);
        return HexFormat.of().withUpperCase().formatHex(buffer);
    }

    private String nsu() {
        return String.valueOf(100000 + random.nextInt(900000));
    }
}
