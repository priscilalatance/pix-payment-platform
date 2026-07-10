package com.comprovante.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.comprovante.infrastructure.messaging.ComprovanteProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Verificacao provider-side do contrato gerado pelo ms-pagamento (consumer),
 * versionado em src/test/resources/pacts. Sobe a aplicacao real e confirma que
 * POST /comprovantes responde conforme o contrato (202 + identificador_comprovante).
 *
 * O ComprovanteProducer e mockado para a verificacao nao exigir RabbitMQ.
 */
@Provider("ms-comprovantes")
@PactFolder("src/test/resources/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComprovanteProviderPactTest {

    @LocalServerPort
    private int port;

    @MockBean
    private ComprovanteProducer comprovanteProducer;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("Comprovante pode ser gerado")
    void comprovantePodeSerGerado() {
        // Nenhum setup de dados: o POST apenas valida e enfileira (producer mockado).
    }
}
