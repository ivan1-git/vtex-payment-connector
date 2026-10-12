package dev.ivanjimenez.connector.acquirer;

import java.math.BigDecimal;

/**
 * El "banco" o procesador real. En este repo solo existe la versión sandbox,
 * pero el servicio no sabe eso: cambiar de procesador es implementar esta interfaz.
 */
public interface Acquirer {

    String name();

    AuthorizationResult authorize(String cardNumber, BigDecimal amount, String currency);
}
