package com.pagamento.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ms-comprovantes")
class ComprovanteContractTest {

    @Pact(provider = "ms-comprovantes", consumer = "ms-pagamento")
    public V4Pact criarComprovantePact(PactDslWithProvider builder) {
        return builder
                .given("Comprovante pode ser gerado")
                .uponReceiving("Requisição para gerar comprovante")
                .path("/comprovantes")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("nome", "Giovanni Vicente")
                        .stringType("tipo_documento", "CPF")
                        .stringType("numero_documento", "50329291076")
                        .stringType("numero_agencia", "2022")
                        .stringType("numero_conta", "00276")
                        .stringType("digito_verificador_conta", "0")
                        .decimalType("valor_transacao", 23.99)
                        .stringType("tipo_chave_pix_destino", "CELULAR")
                        .stringType("chave_pix_destino", "11948755536")
                        .stringType("nome_cliente_destino", "Fernando Augusto")
                        .stringType("identificacao_pix", "Segue pagamento")
                        .stringType("data_hora_transacao", "2022-04-10T20:03:57.116061100")
                )
                .willRespondWith()
                .status(202)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("identificador_comprovante", "b819cc65-f6f0-478c-8bb7-69ee1c4f6402")
                        .stringType("data_hora_requisicao", "2022-04-10T20:03:57.116061100")
                )
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "criarComprovantePact")
    void testCriarComprovante(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

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
                  "data_hora_transacao": "2022-04-10T20:03:57.116061100"
                }
                """;

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/comprovantes",
                HttpMethod.POST,
                entity,
                String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("identificador_comprovante"));
        assertTrue(response.getBody().contains("data_hora_requisicao"));
    }
}
