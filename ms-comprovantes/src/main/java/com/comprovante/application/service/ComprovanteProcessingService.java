package com.comprovante.application.service;

import com.comprovante.dto.ComprovanteMessage;
import com.comprovante.dto.PagamentoRealizadoEvent;
import com.comprovante.infrastructure.messaging.PagamentoEventPublisher;
import com.comprovante.infrastructure.messaging.SagaCallbackPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComprovanteProcessingService {

    private final ComprovantePersistenceService persistenceService;
    private final SagaCallbackPublisher sagaCallbackPublisher;
    private final PagamentoEventPublisher pagamentoEventPublisher;

    public void processar(ComprovanteMessage message) {
        String identificador = message.getIdentificadorComprovante();

        // Idempotencia: protege contra redelivery do RabbitMQ
        if (persistenceService.existe(UUID.fromString(identificador))) {
            log.warn("Comprovante {} ja gravado. Ignorando mensagem duplicada.", identificador);
            return;
        }

        try {
            persistenceService.gravar(message);
            log.info("Comprovante {} gravado no banco", identificador);

            // Avisa o ms-pagamento (SAGA) e o ms-notificacao (Kafka)
            sagaCallbackPublisher.sucesso(identificador);
            pagamentoEventPublisher.publicar(construirEvento(message));

        } catch (Exception e) {
            log.error("Falha ao gravar comprovante {}: {}", identificador, e.getMessage());
            sagaCallbackPublisher.falha(identificador, e.getMessage());
        }
    }

    private PagamentoRealizadoEvent construirEvento(ComprovanteMessage message) {
        return PagamentoRealizadoEvent.builder()
                .identificadorComprovante(message.getIdentificadorComprovante())
                .nome(message.getNome())
                .numeroDocumento(message.getNumeroDocumento())
                .valorTransacao(message.getValorTransacao())
                .chavePixDestino(message.getChavePixDestino())
                .nomeClienteDestino(message.getNomeClienteDestino())
                .dataHoraTransacao(message.getDataHoraTransacao())
                .build();
    }
}
