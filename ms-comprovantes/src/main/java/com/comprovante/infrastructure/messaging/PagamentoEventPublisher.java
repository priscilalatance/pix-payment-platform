package com.comprovante.infrastructure.messaging;

import com.comprovante.dto.PagamentoRealizadoEvent;
import com.comprovante.infrastructure.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PagamentoEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publicar(PagamentoRealizadoEvent evento) {
        kafkaTemplate.send(KafkaConfig.PAGAMENTO_REALIZADO_TOPIC, evento.getIdentificadorComprovante(), evento);
        log.info("Evento pagamento.realizado publicado para comprovante {}", evento.getIdentificadorComprovante());
    }
}
