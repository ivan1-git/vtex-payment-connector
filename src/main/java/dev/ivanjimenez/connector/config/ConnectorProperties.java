package dev.ivanjimenez.connector.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Todo lo configurable del conector vive aquí, para no regar @Value por el código.
 *
 * @param appKey               valor esperado en el header X-VTEX-API-AppKey
 * @param appToken             valor esperado en el header X-VTEX-API-AppToken
 * @param asyncDelay           cuánto tarda el "banco" en resolver un pago asíncrono
 * @param callbackMaxAttempts  intentos para avisarle a VTEX antes de rendirse
 * @param delayToCancel        segundos que VTEX espera un pago "undefined" antes de cancelarlo
 */
@ConfigurationProperties(prefix = "connector")
public record ConnectorProperties(
        String appKey,
        String appToken,
        Duration asyncDelay,
        int callbackMaxAttempts,
        long delayToCancel) {
}
