package com.comprovante.service;

import com.comprovante.application.service.ComprovantePersistenceService;
import com.comprovante.application.service.ComprovanteProcessingService;
import com.comprovante.dto.ComprovanteMessage;
import com.comprovante.dto.PagamentoRealizadoEvent;
import com.comprovante.infrastructure.messaging.PagamentoEventPublisher;
import com.comprovante.infrastructure.messaging.SagaCallbackPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComprovanteProcessingServiceTest {

    @Mock
    private ComprovantePersistenceService persistenceService;

    @Mock
    private SagaCallbackPublisher sagaCallbackPublisher;

    @Mock
    private PagamentoEventPublisher pagamentoEventPublisher;

    @InjectMocks
    private ComprovanteProcessingService processingService;

    private ComprovanteMessage novaMensagem() {
        return ComprovanteMessage.builder()
                .identificadorComprovante(UUID.randomUUID().toString())
                .nome("Giovanni Vicente")
                .numeroDocumento("50329291076")
                .valorTransacao(new BigDecimal("23.99"))
                .chavePixDestino("11948755536")
                .nomeClienteDestino("Fernando Augusto")
                .dataHoraTransacao(LocalDateTime.now())
                .build();
    }

    @Test
    void deveGravarEPublicarSucessoEKafka() {
        ComprovanteMessage message = novaMensagem();
        when(persistenceService.existe(any(UUID.class))).thenReturn(false);

        processingService.processar(message);

        verify(persistenceService).gravar(message);
        verify(sagaCallbackPublisher).sucesso(message.getIdentificadorComprovante());
        verify(pagamentoEventPublisher).publicar(any(PagamentoRealizadoEvent.class));
        verify(sagaCallbackPublisher, never()).falha(anyString(), anyString());
    }

    @Test
    void devePublicarFalhaQuandoGravacaoLancaExcecao() {
        ComprovanteMessage message = novaMensagem();
        when(persistenceService.existe(any(UUID.class))).thenReturn(false);
        when(persistenceService.gravar(message)).thenThrow(new RuntimeException("erro no banco"));

        processingService.processar(message);

        verify(sagaCallbackPublisher).falha(eq(message.getIdentificadorComprovante()), anyString());
        verify(sagaCallbackPublisher, never()).sucesso(anyString());
        verify(pagamentoEventPublisher, never()).publicar(any());
    }

    @Test
    void deveIgnorarMensagemQuandoComprovanteJaExiste() {
        ComprovanteMessage message = novaMensagem();
        when(persistenceService.existe(any(UUID.class))).thenReturn(true);

        processingService.processar(message);

        verify(persistenceService, never()).gravar(any());
        verify(sagaCallbackPublisher, never()).sucesso(anyString());
        verify(sagaCallbackPublisher, never()).falha(anyString(), anyString());
        verify(pagamentoEventPublisher, never()).publicar(any());
    }
}
