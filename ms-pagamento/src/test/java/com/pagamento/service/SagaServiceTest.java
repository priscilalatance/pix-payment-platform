package com.pagamento.service;

import com.pagamento.application.service.SagaService;
import com.pagamento.domain.entity.Fatura;
import com.pagamento.domain.entity.Pagamento;
import com.pagamento.domain.enums.StatusFatura;
import com.pagamento.domain.repository.FaturaRepository;
import com.pagamento.domain.repository.PagamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaServiceTest {

    @Mock
    private FaturaRepository faturaRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @InjectMocks
    private SagaService sagaService;

    @Test
    void deveCompletarSagaComSucesso() {
        String identificadorComprovante = "b819cc65-f6f0-478c-8bb7-69ee1c4f6402";
        Fatura fatura = Fatura.builder()
                .id(UUID.randomUUID())
                .status(StatusFatura.COMPROVANTE_SOLICITADO)
                .timeoutEm(LocalDateTime.now().plusMinutes(30))
                .build();
        Pagamento pagamento = Pagamento.builder()
                .id(UUID.randomUUID())
                .fatura(fatura)
                .identificadorComprovante(identificadorComprovante)
                .build();

        when(pagamentoRepository.findByIdentificadorComprovante(identificadorComprovante))
                .thenReturn(Optional.of(pagamento));

        sagaService.completarSaga(identificadorComprovante);

        assertEquals(StatusFatura.PAGA, fatura.getStatus());
        assertNull(fatura.getTimeoutEm());
        verify(faturaRepository).save(fatura);
    }

    @Test
    void deveCompensarSagaQuandoFalha() {
        String identificadorComprovante = "b819cc65-f6f0-478c-8bb7-69ee1c4f6402";
        Fatura fatura = Fatura.builder()
                .id(UUID.randomUUID())
                .status(StatusFatura.COMPROVANTE_SOLICITADO)
                .timeoutEm(LocalDateTime.now().plusMinutes(30))
                .build();
        Pagamento pagamento = Pagamento.builder()
                .id(UUID.randomUUID())
                .fatura(fatura)
                .identificadorComprovante(identificadorComprovante)
                .build();

        when(pagamentoRepository.findByIdentificadorComprovante(identificadorComprovante))
                .thenReturn(Optional.of(pagamento));

        sagaService.compensarSaga(identificadorComprovante, "Erro ao gravar no banco");

        assertEquals(StatusFatura.FALHA, fatura.getStatus());
        assertNull(fatura.getTimeoutEm());
        verify(faturaRepository).save(fatura);
    }

    @Test
    void deveIgnorarCallbackSucessoQuandoFaturaJaProcessada() {
        String identificadorComprovante = "b819cc65-f6f0-478c-8bb7-69ee1c4f6402";
        Fatura fatura = Fatura.builder()
                .id(UUID.randomUUID())
                .status(StatusFatura.PAGA)
                .build();
        Pagamento pagamento = Pagamento.builder()
                .id(UUID.randomUUID())
                .fatura(fatura)
                .identificadorComprovante(identificadorComprovante)
                .build();

        when(pagamentoRepository.findByIdentificadorComprovante(identificadorComprovante))
                .thenReturn(Optional.of(pagamento));

        sagaService.completarSaga(identificadorComprovante);

        assertEquals(StatusFatura.PAGA, fatura.getStatus());
        verify(faturaRepository, never()).save(any());
    }

    @Test
    void deveIgnorarCallbackFalhaQuandoFaturaJaProcessada() {
        String identificadorComprovante = "b819cc65-f6f0-478c-8bb7-69ee1c4f6402";
        Fatura fatura = Fatura.builder()
                .id(UUID.randomUUID())
                .status(StatusFatura.PAGA)
                .build();
        Pagamento pagamento = Pagamento.builder()
                .id(UUID.randomUUID())
                .fatura(fatura)
                .identificadorComprovante(identificadorComprovante)
                .build();

        when(pagamentoRepository.findByIdentificadorComprovante(identificadorComprovante))
                .thenReturn(Optional.of(pagamento));

        sagaService.compensarSaga(identificadorComprovante, "motivo");

        assertEquals(StatusFatura.PAGA, fatura.getStatus());
        verify(faturaRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoPagamentoNaoEncontradoNoCompletar() {
        String identificadorComprovante = "inexistente";

        when(pagamentoRepository.findByIdentificadorComprovante(identificadorComprovante))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> sagaService.completarSaga(identificadorComprovante));
        verify(faturaRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoPagamentoNaoEncontradoNoCompensar() {
        String identificadorComprovante = "inexistente";

        when(pagamentoRepository.findByIdentificadorComprovante(identificadorComprovante))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> sagaService.compensarSaga(identificadorComprovante, "motivo"));
        verify(faturaRepository, never()).save(any());
    }
}
