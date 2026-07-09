package com.pagamento.infrastructure.messaging;

import com.pagamento.application.service.SagaService;
import com.pagamento.dto.SagaCallbackMessage;
import com.pagamento.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCallbackListener {

    private final SagaService sagaService;

    @RabbitListener(queues = RabbitMQConfig.SAGA_SUCESSO_QUEUE)
    public void onSucesso(SagaCallbackMessage message) {
        log.info("SAGA callback SUCESSO recebido: {}", message.getIdentificadorComprovante());
        sagaService.completarSaga(message.getIdentificadorComprovante());
    }

    @RabbitListener(queues = RabbitMQConfig.SAGA_FALHA_QUEUE)
    public void onFalha(SagaCallbackMessage message) {
        log.info("SAGA callback FALHA recebido: {} - motivo: {}", message.getIdentificadorComprovante(), message.getMotivo());
        sagaService.compensarSaga(message.getIdentificadorComprovante(), message.getMotivo());
    }
}
