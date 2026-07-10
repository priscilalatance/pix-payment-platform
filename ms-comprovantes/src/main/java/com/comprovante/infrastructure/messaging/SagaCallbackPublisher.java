package com.comprovante.infrastructure.messaging;

import com.comprovante.dto.SagaCallbackMessage;
import com.comprovante.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCallbackPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void sucesso(String identificadorComprovante) {
        SagaCallbackMessage message = SagaCallbackMessage.builder()
                .identificadorComprovante(identificadorComprovante)
                .status("SUCESSO")
                .dataHoraProcessamento(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.SAGA_SUCESSO_ROUTING_KEY,
                message);
        log.info("Callback SAGA SUCESSO publicado para comprovante {}", identificadorComprovante);
    }

    public void falha(String identificadorComprovante, String motivo) {
        SagaCallbackMessage message = SagaCallbackMessage.builder()
                .identificadorComprovante(identificadorComprovante)
                .status("FALHA")
                .motivo(motivo)
                .dataHoraProcessamento(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.SAGA_FALHA_ROUTING_KEY,
                message);
        log.warn("Callback SAGA FALHA publicado para comprovante {}. Motivo: {}", identificadorComprovante, motivo);
    }
}
