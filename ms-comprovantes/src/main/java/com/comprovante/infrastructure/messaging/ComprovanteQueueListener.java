package com.comprovante.infrastructure.messaging;

import com.comprovante.application.service.ComprovanteProcessingService;
import com.comprovante.dto.ComprovanteMessage;
import com.comprovante.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComprovanteQueueListener {

    private final ComprovanteProcessingService processingService;

    @RabbitListener(queues = RabbitMQConfig.COMPROVANTE_GERAR_QUEUE)
    public void onMessage(ComprovanteMessage message) {
        log.info("Mensagem recebida em {}: comprovante {}",
                RabbitMQConfig.COMPROVANTE_GERAR_QUEUE, message.getIdentificadorComprovante());
        processingService.processar(message);
    }
}
