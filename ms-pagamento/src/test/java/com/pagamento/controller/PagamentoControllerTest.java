package com.pagamento.controller;

import com.pagamento.application.service.PagamentoService;
import com.pagamento.domain.enums.StatusFatura;
import com.pagamento.dto.PagamentoRequest;
import com.pagamento.dto.PagamentoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagamentoController.class)
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagamentoService pagamentoService;

    @Test
    void deveRetornar202QuandoPagamentoProcessado() throws Exception {
        PagamentoResponse response = PagamentoResponse.builder()
                .faturaId(UUID.randomUUID())
                .status(StatusFatura.COMPROVANTE_SOLICITADO.name())
                .identificadorComprovante("b819cc65-f6f0-478c-8bb7-69ee1c4f6402")
                .dataHoraRequisicao(LocalDateTime.of(2022, 4, 10, 20, 3, 57))
                .build();

        when(pagamentoService.processarPagamento(any(PagamentoRequest.class))).thenReturn(response);

        String requestBody = """
                {
                  "nome": "Giovanni Vicente",
                  "tipo_documento": "CPF",
                  "numero_documento": "50329291076",
                  "numero_agencia": "2022",
                  "numero_conta": "00276",
                  "digito_verificador_conta": "0",
                  "valor_transacao": 23.99,
                  "tipo_chave_pix_destino": "CELULAR",
                  "chave_pix_destino": "11948755536",
                  "nome_cliente_destino": "Fernando Augusto",
                  "identificacao_pix": "Segue pagamento",
                  "data_hora_transacao": "2022-04-10T20:03:57"
                }
                """;

        mockMvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("COMPROVANTE_SOLICITADO"))
                .andExpect(jsonPath("$.identificador_comprovante").value("b819cc65-f6f0-478c-8bb7-69ee1c4f6402"));
    }

    @Test
    void deveRetornar400QuandoCamposObrigatoriosFaltando() throws Exception {
        String requestBody = """
                {
                  "nome": "",
                  "valor_transacao": null
                }
                """;

        mockMvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    void deveRetornar400QuandoBodyVazio() throws Exception {
        mockMvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
