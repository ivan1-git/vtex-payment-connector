package dev.ivanjimenez.connector.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import dev.ivanjimenez.connector.TestPayloads;
import dev.ivanjimenez.connector.acquirer.SandboxAcquirer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = "connector.job-interval-ms=3600000")
@AutoConfigureMockMvc
class PaymentApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void manifestIsPublicAndListsTheCardBrands() throws Exception {
        mvc.perform(get("/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethods", hasSize(3)))
                .andExpect(jsonPath("$.paymentMethods[0].name").value("American Express"));
    }

    @Test
    void rejectsCallsWithoutVtexCredentials() throws Exception {
        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header("X-VTEX-API-AppKey", "demo-key")
                        .header("X-VTEX-API-AppToken", "otro-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approvesAPaymentAndReturnsTheSameResponseOnRetry() throws Exception {
        String paymentId = newId();

        String first = createPayment(paymentId, SandboxAcquirer.CARD_APPROVED, "1499.90")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.authorizationId", startsWith("AUT-")))
                .andExpect(jsonPath("$.acquirer").value("SandboxBank"))
                .andExpect(jsonPath("$.delayToCancel").value(21600))
                .andReturn().getResponse().getContentAsString();

        String retry = createPayment(paymentId, SandboxAcquirer.CARD_APPROVED, "1499.90")
                .andReturn().getResponse().getContentAsString();

        assertThat(retry).isEqualTo(first);
    }

    @Test
    void deniesTheDeniedTestCard() throws Exception {
        createPayment(newId(), SandboxAcquirer.CARD_DENIED, "100.00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("denied"))
                .andExpect(jsonPath("$.code").value("05"));
    }

    @Test
    void deniesPaymentMethodsOutsideTheManifest() throws Exception {
        String body = TestPayloads.payment(newId(), SandboxAcquirer.CARD_APPROVED, "100.00").replace("\"Visa\"", "\"Pix\"");

        mvc.perform(authorized(post("/payments")).content(body))
                .andExpect(jsonPath("$.status").value("denied"))
                .andExpect(jsonPath("$.code").value("unsupported-method"));
    }

    @Test
    void validatesTheRequestBody() throws Exception {
        mvc.perform(authorized(post("/payments")).content("{\"paymentId\":\"x\",\"value\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid-request"));

        mvc.perform(authorized(post("/payments")).content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid-json"));
    }

    @Test
    void settlesPartiallyAndThenRefunds() throws Exception {
        String paymentId = newId();
        createPayment(paymentId, SandboxAcquirer.CARD_APPROVED, "1000.00");

        mvc.perform(authorized(post("/payments/{id}/settlements", paymentId))
                        .content("{\"requestId\":\"s-1\",\"value\":600.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settleId", startsWith("STL-")))
                .andExpect(jsonPath("$.value").value(600.00));

        // Intentar capturar más de lo que queda autorizado
        mvc.perform(authorized(post("/payments/{id}/settlements", paymentId))
                        .content("{\"requestId\":\"s-2\",\"value\":500.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.settleId").value(nullValue()))
                .andExpect(jsonPath("$.code").value("amount-exceeded"));

        mvc.perform(authorized(post("/payments/{id}/refunds", paymentId))
                        .content("{\"requestId\":\"r-1\",\"value\":150.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId", startsWith("REF-")));
    }

    @Test
    void aRetriedSettlementDoesNotChargeTwice() throws Exception {
        String paymentId = newId();
        createPayment(paymentId, SandboxAcquirer.CARD_APPROVED, "300.00");
        String settlement = "{\"requestId\":\"same-request\",\"value\":300.00}";

        String first = mvc.perform(authorized(post("/payments/{id}/settlements", paymentId)).content(settlement))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Si se aplicara dos veces, esta segunda llamada fallaría por monto excedido
        String retry = mvc.perform(authorized(post("/payments/{id}/settlements", paymentId)).content(settlement))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(retry).isEqualTo(first);
    }

    @Test
    void cancelsAnApprovedPaymentButNotASettledOne() throws Exception {
        String toCancel = newId();
        createPayment(toCancel, SandboxAcquirer.CARD_APPROVED, "250.00");

        mvc.perform(authorized(post("/payments/{id}/cancellations", toCancel)).content("{\"requestId\":\"c-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellationId", startsWith("CAN-")));

        String settled = newId();
        createPayment(settled, SandboxAcquirer.CARD_APPROVED, "250.00");
        mvc.perform(authorized(post("/payments/{id}/settlements", settled)).content("{\"requestId\":\"s-1\",\"value\":250}"))
                .andExpect(status().isOk());

        mvc.perform(authorized(post("/payments/{id}/cancellations", settled)).content("{\"requestId\":\"c-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cancellationId").value(nullValue()))
                .andExpect(jsonPath("$.code").value("payment-settled"));
    }

    @Test
    void operationsOnUnknownPaymentsReturn404() throws Exception {
        mvc.perform(authorized(post("/payments/{id}/cancellations", "no-existe")).content("{\"requestId\":\"c-1\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("payment-not-found"));
    }

    private ResultActions createPayment(String paymentId, String card, String value) throws Exception {
        return mvc.perform(authorized(post("/payments")).content(TestPayloads.payment(paymentId, card, value)));
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.contentType(MediaType.APPLICATION_JSON)
                .header("X-VTEX-API-AppKey", "demo-key")
                .header("X-VTEX-API-AppToken", "demo-token");
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
