package dev.ivanjimenez.connector.web;

import java.util.List;

import dev.ivanjimenez.connector.service.PaymentService;
import dev.ivanjimenez.connector.web.dto.ManifestResponse;
import dev.ivanjimenez.connector.web.dto.ManifestResponse.PaymentMethod;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** VTEX lee este endpoint para saber qué métodos de pago ofrece el conector. */
@RestController
public class ManifestController {

    @GetMapping("/manifest")
    public ManifestResponse manifest() {
        List<PaymentMethod> methods = PaymentService.SUPPORTED_METHODS.stream()
                .sorted()
                .map(name -> new PaymentMethod(name, "disabled"))
                .toList();
        return new ManifestResponse(methods);
    }
}
