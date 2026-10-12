package dev.ivanjimenez.connector.web.dto;

import java.util.List;

public record ManifestResponse(List<PaymentMethod> paymentMethods) {

    public record PaymentMethod(String name, String allowsSplit) {
    }
}
