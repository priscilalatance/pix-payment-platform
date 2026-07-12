package com.notificacao.service;

import com.notificacao.application.service.NotificacaoService;
import com.notificacao.domain.entity.Notificacao;
import com.notificacao.domain.enums.StatusNotificacao;
import com.notificacao.domain.repository.NotificacaoRepository;
import com.notificacao.dto.PagamentoRealizadoEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @InjectMocks
    private NotificacaoService notificacaoService;

    private PagamentoRealizadoEvent novoEvento() {
        return new PagamentoRealizadoEvent(
                UUID.randomUUID(),
                "Giovanni Vicente",
                "50329291076",
                new BigDecimal("23.99"),
                "11948755536",
                "Fernando Augusto",
                LocalDateTime.now()
        );
    }

    @Test
    void deveGravarNotificacaoEnviadaNoHappyPath() {
        PagamentoRealizadoEvent evento = novoEvento();
        when(notificacaoRepository.existsByIdentificadorComprovante(evento.identificadorComprovante()))
                .thenReturn(false);

        notificacaoService.processar(evento);

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).save(captor.capture());
        Notificacao salva = captor.getValue();
        assertThat(salva.getStatus()).isEqualTo(StatusNotificacao.ENVIADA);
        assertThat(salva.getIdentificadorComprovante()).isEqualTo(evento.identificadorComprovante());
        assertThat(salva.getDataEnvio()).isNotNull();
    }

    @Test
    void deveIgnorarQuandoNotificacaoJaExiste() {
        PagamentoRealizadoEvent evento = novoEvento();
        when(notificacaoRepository.existsByIdentificadorComprovante(evento.identificadorComprovante()))
                .thenReturn(true);

        notificacaoService.processar(evento);

        verify(notificacaoRepository, never()).save(any());
    }

    @Test
    void deveGravarNotificacaoFalhaEmRegistrarFalha() {
        PagamentoRealizadoEvent evento = novoEvento();

        notificacaoService.registrarFalha(evento, 4);

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).save(captor.capture());
        Notificacao salva = captor.getValue();
        assertThat(salva.getStatus()).isEqualTo(StatusNotificacao.FALHA);
        assertThat(salva.getTentativas()).isEqualTo(4);
        assertThat(salva.getDataEnvio()).isNull();
    }
}
