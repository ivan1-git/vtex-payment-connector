package dev.ivanjimenez.connector.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import dev.ivanjimenez.connector.TestPayloads;
import dev.ivanjimenez.connector.acquirer.SandboxAcquirer;
import dev.ivanjimenez.connector.service.PaymentService;
import dev.ivanjimenez.connector.web.dto.PaymentResponse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pagos que el banco no resuelve al momento: VTEX recibe "undefined"
 * y después nos toca avisarle por el callbackUrl.
 */
@SpringBootTest(properties = {
        "connector.async-delay=0s",
        "connector.callback-max-attempts=3",
        "connector.job-interval-ms=3600000",
        "spring.datasource.url=jdbc:h2:mem:async-flow;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class AsyncPaymentFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    PendingPaymentsJob job;

    @Autowired
    PaymentService service;

    @MockitoBean
    CallbackNotifier notifier;

    @Test
    void notifiesVtexWhenTheBankApprovesLater() throws Exception {
        when(notifier.notify(anyString(), any())).thenReturn(true);
        String paymentId = create(SandboxAcquirer.CARD_ASYNC_APPROVED);

        job.run();

        ArgumentCaptor<PaymentResponse> sent = ArgumentCaptor.forClass(PaymentResponse.class);
        verify(notifier).notify(eq("http://localhost:9999/callback?X-VTEX-signature=abc"), sent.capture());
        assertThat(sent.getValue().paymentId()).isEqualTo(paymentId);
        assertThat(sent.getValue().status()).isEqualTo("approved");

        // Ya se entregó: otra corrida del job no debe volver a avisar
        job.run();
        verify(notifier, times(1)).notify(anyString(), any());
    }

    @Test
    void notifiesDenialsToo() throws Exception {
        when(notifier.notify(anyString(), any())).thenReturn(true);
        String paymentId = create(SandboxAcquirer.CARD_ASYNC_DENIED);

        job.run();

        ArgumentCaptor<PaymentResponse> sent = ArgumentCaptor.forClass(PaymentResponse.class);
        verify(notifier).notify(anyString(), sent.capture());
        assertThat(sent.getValue().paymentId()).isEqualTo(paymentId);
        assertThat(sent.getValue().status()).isEqualTo("denied");
    }

    @Test
    void retriesTheCallbackUntilTheLimit() throws Exception {
        when(notifier.notify(anyString(), any())).thenReturn(false);
        String paymentId = create(SandboxAcquirer.CARD_ASYNC_APPROVED);

        for (int i = 0; i < 5; i++) {
            job.run();
        }

        verify(notifier, times(3)).notify(anyString(), any());
        assertThat(service.find(paymentId).getCallbackAttempts()).isEqualTo(3);
        assertThat(service.find(paymentId).isCallbackPending()).isFalse();
    }

    @Test
    void aCanceledPendingPaymentIsNeverResolved() throws Exception {
        String paymentId = create(SandboxAcquirer.CARD_ASYNC_APPROVED);
        mvc.perform(post("/payments/{id}/cancellations", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-VTEX-API-AppKey", "demo-key")
                        .header("X-VTEX-API-AppToken", "demo-token")
                        .content("{\"requestId\":\"c-1\"}"))
                .andExpect(status().isOk());

        job.run();

        verify(notifier, never()).notify(anyString(), any());
    }

    private String create(String card) throws Exception {
        String paymentId = UUID.randomUUID().toString();
        mvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-VTEX-API-AppKey", "demo-key")
                        .header("X-VTEX-API-AppToken", "demo-token")
                        .content(TestPayloads.payment(paymentId, card, "899.00")))
                .andExpect(jsonPath("$.status").value("undefined"));
        return paymentId;
    }
}
