package dev.ivanjimenez.connector.domain;

/**
 * Una regla de negocio impidió la operación (capturar de más, cancelar algo ya capturado, etc.).
 * No es un error del sistema: VTEX debe recibir una respuesta clara con el motivo.
 */
public class OperationRejectedException extends RuntimeException {

    private final String code;

    public OperationRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
