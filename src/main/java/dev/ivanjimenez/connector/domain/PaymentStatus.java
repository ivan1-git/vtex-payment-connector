package dev.ivanjimenez.connector.domain;

/**
 * Los tres estados que VTEX entiende como respuesta de una autorización.
 * UNDEFINED significa "todavía no sé": VTEX espera un callback o vuelve a preguntar.
 */
public enum PaymentStatus {
    APPROVED,
    DENIED,
    UNDEFINED;

    public String toVtex() {
        return name().toLowerCase();
    }
}
