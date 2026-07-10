package com.comprovante.infrastructure.messaging;

import com.comprovante.dto.ComprovanteMessage;
import com.comprovante.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComprovanteProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publicar(ComprovanteMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.COMPROVANTE_EXCHANGE,
                RabbitMQConfig.COMPROVANTE_ROUTING_KEY,
                message);
        log.info("Comprovante {} enfileirado em {}", message.getIdentificadorComprovante(), RabbitMQConfig.COMPROVANTE_GERAR_QUEUE);
    }
}
