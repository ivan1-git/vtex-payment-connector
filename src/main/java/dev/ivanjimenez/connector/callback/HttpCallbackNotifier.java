package dev.ivanjimenez.connector.callback;

import java.net.URI;

import dev.ivanjimenez.connector.web.dto.PaymentResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpCallbackNotifier implements CallbackNotifier {

    private static final Logger log = LoggerFactory.getLogger(HttpCallbackNotifier.class);

    private final RestClient restClient;

    public HttpCallbackNotifier(RestClient callbackRestClient) {
        this.restClient = callbackRestClient;
    }

    @Override
    public boolean notify(String callbackUrl, PaymentResponse payment) {
        try {
            // URI.create y no un template: la URL trae una firma que VTEX valida y no se debe re-codificar
            restClient.post()
                    .uri(URI.create(callbackUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payment)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Callback enviado para el pago {} con estado {}", payment.paymentId(), payment.status());
            return true;
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("Falló el callback del pago {}: {}", payment.paymentId(), e.getMessage());
            return false;
        }
    }
}
