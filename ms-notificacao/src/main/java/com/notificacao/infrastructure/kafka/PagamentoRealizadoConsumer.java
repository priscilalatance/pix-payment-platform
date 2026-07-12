package com.notificacao.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificacao.application.service.NotificacaoService;
import com.notificacao.dto.PagamentoRealizadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PagamentoRealizadoConsumer {

    private final ObjectMapper objectMapper;
    private final NotificacaoService notificacaoService;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 4000
            ),
            dltTopicSuffix = "-dlt",
            // Erro de desserializacao nao e transitorio: vai direto para a DLT, sem retentar.
            exclude = { IllegalArgumentException.class }
    )
    @KafkaListener(
            topics = "${app.kafka.topic.pagamento-realizado}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumir(String mensagem) {
        try {
            PagamentoRealizadoEvent evento =
                    objectMapper.readValue(mensagem, PagamentoRealizadoEvent.class);

            log.info(
                    "Evento de pagamento recebido. Comprovante: {}",
                    evento.identificadorComprovante()
            );

            notificacaoService.processar(evento);

        } catch (JsonProcessingException exception) {
            log.error(
                    "Erro ao desserializar evento de pagamento: {}",
                    mensagem,
                    exception
            );

            throw new IllegalArgumentException(
                    "Mensagem Kafka invalida",
                    exception
            );
        }
    }

    @DltHandler
    public void processarDlt(String mensagem) {
        log.error(
                "Mensagem enviada para a DLT apos esgotar as tentativas: {}",
                mensagem
        );

        try {
            PagamentoRealizadoEvent evento = objectMapper.readValue(mensagem, PagamentoRealizadoEvent.class);
            notificacaoService.registrarFalha(evento, 4);
            log.info("Mensagem persistida na DLT com status FALHA para o comprovante {}", evento.identificadorComprovante());
        } catch (JsonProcessingException exception) {
            log.error(
                    "Payload invalido para persistencia na DLT e nao pode ser persistido: {}",
                    mensagem,
                    exception
            );
        }
    }
}