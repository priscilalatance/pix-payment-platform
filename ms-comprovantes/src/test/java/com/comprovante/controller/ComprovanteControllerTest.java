package com.comprovante.controller;

import com.comprovante.application.exception.ComprovanteNaoEncontradoException;
import com.comprovante.application.service.ComprovanteConsultaService;
import com.comprovante.application.service.ComprovanteService;
import com.comprovante.dto.ComprovanteConsultaResponse;
import com.comprovante.dto.ComprovanteRequest;
import com.comprovante.dto.ComprovanteResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ComprovanteController.class)
class ComprovanteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComprovanteService comprovanteService;

    @MockBean
    private ComprovanteConsultaService comprovanteConsultaService;

    private static final String PAYLOAD_VALIDO = """
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
              "data_hora_transacao": "2022-04-10T20:03:57.116061100"
            }
            """;

    @Test
    void deveRetornar202AoGerarComprovante() throws Exception {
        ComprovanteResponse response = ComprovanteResponse.builder()
                .identificadorComprovante("b819cc65-f6f0-478c-8bb7-69ee1c4f6402")
                .dataHoraRequisicao(LocalDateTime.of(2022, 4, 10, 20, 3, 57))
                .build();
        when(comprovanteService.solicitarComprovante(any(ComprovanteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/comprovantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.identificador_comprovante").value("b819cc65-f6f0-478c-8bb7-69ee1c4f6402"))
                .andExpect(jsonPath("$.data_hora_requisicao").exists());
    }

    @Test
    void deveRetornar400QuandoCamposObrigatoriosFaltando() throws Exception {
        String requestBody = """
                {
                  "nome": "",
                  "valor_transacao": null
                }
                """;

        mockMvc.perform(post("/comprovantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    void deveRetornar400QuandoBodyVazio() throws Exception {
        mockMvc.perform(post("/comprovantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar200AoConsultarComprovanteExistente() throws Exception {
        String id = "b819cc65-f6f0-478c-8bb7-69ee1c4f6402";
        ComprovanteConsultaResponse response = ComprovanteConsultaResponse.builder()
                .identificadorComprovante(id)
                .nome("Giovanni Vicente")
                .build();
        when(comprovanteConsultaService.consultar(eq(id))).thenReturn(response);

        mockMvc.perform(get("/comprovantes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador_comprovante").value(id))
                .andExpect(jsonPath("$.nome").value("Giovanni Vicente"));
    }

    @Test
    void deveRetornar404QuandoComprovanteNaoEncontrado() throws Exception {
        String id = "00000000-0000-0000-0000-000000000000";
        when(comprovanteConsultaService.consultar(eq(id)))
                .thenThrow(new ComprovanteNaoEncontradoException(id));

        mockMvc.perform(get("/comprovantes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
