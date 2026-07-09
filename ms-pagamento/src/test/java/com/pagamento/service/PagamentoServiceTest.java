package com.pagamento.service;

import com.pagamento.application.service.FaturaPersistenceService;
import com.pagamento.application.service.PagamentoService;
import com.pagamento.domain.entity.Fatura;
import com.pagamento.domain.entity.Pagamento;
import com.pagamento.domain.enums.StatusFatura;
import com.pagamento.dto.ComprovanteResponse;
import com.pagamento.dto.PagamentoRequest;
import com.pagamento.dto.PagamentoResponse;
import com.pagamento.infrastructure.client.ComprovanteClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private FaturaPersistenceService faturaPersistenceService;

    @Mock
    private ComprovanteClient comprovanteClient;

    @InjectMocks
    private PagamentoService pagamentoService;

    private PagamentoRequest request;
    private Fatura faturaSalva;

    @BeforeEach
    void setUp() {
        request = PagamentoRequest.builder()
                .nome("Giovanni Vicente")
                .tipoDocumento("CPF")
                .numeroDocumento("50329291076")
                .numeroAgencia("2022")
                .numeroConta("00276")
                .digitoVerificadorConta("0")
                .valorTransacao(new BigDecimal("23.99"))
                .tipoChavePixDestino("CELULAR")
                .chavePixDestino("11948755536")
                .nomeClienteDestino("Fernando Augusto")
                .identificacaoPix("Segue pagamento")
                .dataHoraTransacao(LocalDateTime.of(2022, 4, 10, 20, 3, 57))
                .build();

        faturaSalva = Fatura.builder()
                .id(UUID.randomUUID())
                .nome("Giovanni Vicente")
                .status(StatusFatura.PENDENTE)
                .build();
    }

    @Test
    void deveProcessarPagamentoComSucesso() {
        when(faturaPersistenceService.salvar(any(Fatura.class))).thenReturn(faturaSalva);

        ComprovanteResponse comprovanteResponse = new ComprovanteResponse(
                "b819cc65-f6f0-478c-8bb7-69ee1c4f6402",
                LocalDateTime.of(2022, 4, 10, 20, 3, 57));
        when(comprovanteClient.solicitarComprovante(request)).thenReturn(comprovanteResponse);

        Pagamento pagamento = Pagamento.builder().id(UUID.randomUUID()).build();
        when(faturaPersistenceService.criarPagamento(eq(faturaSalva), eq("b819cc65-f6f0-478c-8bb7-69ee1c4f6402"), eq(request)))
                .thenReturn(pagamento);

        PagamentoResponse response = pagamentoService.processarPagamento(request);

        assertEquals(StatusFatura.COMPROVANTE_SOLICITADO.name(), response.getStatus());
        assertEquals("b819cc65-f6f0-478c-8bb7-69ee1c4f6402", response.getIdentificadorComprovante());
        assertEquals(faturaSalva.getId(), response.getFaturaId());

        verify(faturaPersistenceService).salvar(any(Fatura.class));
        verify(faturaPersistenceService).criarPagamento(eq(faturaSalva), eq("b819cc65-f6f0-478c-8bb7-69ee1c4f6402"), eq(request));
        verify(faturaPersistenceService, never()).marcarComoFalha(any());
    }

    @Test
    void deveMarcarComoFalhaQuandoMsComprovantesIndisponivel() {
        when(faturaPersistenceService.salvar(any(Fatura.class))).thenReturn(faturaSalva);
        when(comprovanteClient.solicitarComprovante(request)).thenThrow(new RuntimeException("Connection refused"));

        PagamentoResponse response = pagamentoService.processarPagamento(request);

        assertEquals(StatusFatura.FALHA.name(), response.getStatus());
        assertNull(response.getIdentificadorComprovante());
        assertEquals(faturaSalva.getId(), response.getFaturaId());

        verify(faturaPersistenceService).salvar(any(Fatura.class));
        verify(faturaPersistenceService).marcarComoFalha(faturaSalva);
        verify(faturaPersistenceService, never()).criarPagamento(any(), any(), any());
    }

    @Test
    void deveMarcarComoFalhaQuandoMsComprovantesRetornaErro() {
        when(faturaPersistenceService.salvar(any(Fatura.class))).thenReturn(faturaSalva);
        when(comprovanteClient.solicitarComprovante(request)).thenThrow(new RuntimeException("400 Bad Request"));

        PagamentoResponse response = pagamentoService.processarPagamento(request);

        assertEquals(StatusFatura.FALHA.name(), response.getStatus());
        verify(faturaPersistenceService).marcarComoFalha(faturaSalva);
    }
}
