package dev.ivanjimenez.connector;

/** Cuerpos de petición parecidos a los que manda VTEX, para no repetirlos en cada prueba. */
public final class TestPayloads {

    private TestPayloads() {
    }

    public static String payment(String paymentId, String card, String value) {
        return """
                {
                  "paymentId": "%s",
                  "transactionId": "TX-%s",
                  "orderId": "v123456-01",
                  "reference": "1234567",
                  "paymentMethod": "Visa",
                  "value": %s,
                  "currency": "MXN",
                  "installments": 1,
                  "card": {
                    "holder": "Cliente Prueba",
                    "number": "%s",
                    "csc": "123",
                    "expiration": { "month": "12", "year": "2030" }
                  },
                  "callbackUrl": "http://localhost:9999/callback?X-VTEX-signature=abc",
                  "miniCart": { "buyer": { "firstName": "Campo", "lastName": "Ignorado" } }
                }
                """.formatted(paymentId, paymentId, value, card);
    }

}
