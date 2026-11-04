package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookCommand;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookResult;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookService;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookControllerTest {

    @Mock
    private ProcessMercadoPagoWebhookService webhookService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MercadoPagoWebhookController controller = new MercadoPagoWebhookController(webhookService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /webhook devuelve 200 OK cuando la notificación es válida y procesada")
    void shouldReturn200OnValidWebhook() throws Exception {
        when(webhookService.execute(any(ProcessMercadoPagoWebhookCommand.class))).thenReturn(
                ProcessMercadoPagoWebhookResult.success(
                        UUID.randomUUID(), "123456789", "APPROVED", "APPROVED", "Pago procesado"
                )
        );

        mockMvc.perform(post("/api/v1/checkout/mercado-pago/webhook?data.id=123456789&type=payment")
                        .header("x-signature", "ts=1700000000,v1=validhash")
                        .header("x-request-id", "req-111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"payment.created\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.processed").value(true));
    }

    @Test
    @DisplayName("POST /webhook devuelve 401 Unauthorized ante firma inválida")
    void shouldReturn401OnInvalidSignature() throws Exception {
        when(webhookService.execute(any(ProcessMercadoPagoWebhookCommand.class)))
                .thenThrow(new SecurityException("Firma criptográfica x-signature inválida"));

        mockMvc.perform(post("/api/v1/checkout/mercado-pago/webhook?data.id=123456789&type=payment")
                        .header("x-signature", "ts=1700000000,v1=badhash")
                        .header("x-request-id", "req-111"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
    }

    @Test
    @DisplayName("POST /webhook devuelve 400 Bad Request cuando faltan datos obligatorios")
    void shouldReturn400OnMissingDataId() throws Exception {
        when(webhookService.execute(any(ProcessMercadoPagoWebhookCommand.class)))
                .thenThrow(new IllegalArgumentException("Falta data.id"));

        mockMvc.perform(post("/api/v1/checkout/mercado-pago/webhook"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_WEBHOOK_REQUEST"));
    }

    @Test
    @DisplayName("POST /webhook devuelve 409 Conflict ante discrepancias financieras")
    void shouldReturn409OnFinancialConflict() throws Exception {
        when(webhookService.execute(any(ProcessMercadoPagoWebhookCommand.class)))
                .thenThrow(new ConflictException("PAYMENT_AMOUNT_MISMATCH", "Discrepancia en monto"));

        mockMvc.perform(post("/api/v1/checkout/mercado-pago/webhook?data.id=123456789&type=payment")
                        .header("x-signature", "ts=1700000000,v1=validhash")
                        .header("x-request-id", "req-111"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_AMOUNT_MISMATCH"));
    }

}
