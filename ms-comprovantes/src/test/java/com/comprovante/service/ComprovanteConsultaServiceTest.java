package com.comprovante.service;

import com.comprovante.application.exception.ComprovanteNaoEncontradoException;
import com.comprovante.application.service.ComprovanteConsultaService;
import com.comprovante.domain.entity.Comprovante;
import com.comprovante.domain.repository.ComprovanteRepository;
import com.comprovante.domain.valueobject.DadosDestinatario;
import com.comprovante.domain.valueobject.DadosTransacao;
import com.comprovante.dto.ComprovanteConsultaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComprovanteConsultaServiceTest {

    @Mock
    private RedisTemplate<String, ComprovanteConsultaResponse> redisTemplate;

    @Mock
    private ValueOperations<String, ComprovanteConsultaResponse> valueOperations;

    @Mock
    private ComprovanteRepository comprovanteRepository;

    private ComprovanteConsultaService consultaService;

    private final String id = "b819cc65-f6f0-478c-8bb7-69ee1c4f6402";
    private final String cacheKey = "comprovante:" + id;

    @BeforeEach
    void setUp() {
        // max-tentativas=3, delay=1ms, ttl=10min
        consultaService = new ComprovanteConsultaService(redisTemplate, comprovanteRepository, 3, 1, 10);
    }

    private Comprovante comprovanteEntity() {
        return Comprovante.builder()
                .id(UUID.fromString(id))
                .nome("Giovanni Vicente")
                .dadosTransacao(new DadosTransacao(new BigDecimal("23.99"), "Segue pagamento", LocalDateTime.now()))
                .dadosDestinatario(new DadosDestinatario("CELULAR", "11948755536", "Fernando Augusto"))
                .build();
    }

    @Test
    void deveRetornarDoCacheQuandoPresente() {
        ComprovanteConsultaResponse cacheado = ComprovanteConsultaResponse.builder()
                .identificadorComprovante(id).nome("Giovanni Vicente").build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(cacheado);

        ComprovanteConsultaResponse resultado = consultaService.consultar(id);

        assertEquals(id, resultado.getIdentificadorComprovante());
        verify(comprovanteRepository, never()).findById(any());
    }

    @Test
    void deveBuscarNoBancoQuandoCacheMissECachear() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(comprovanteRepository.findById(UUID.fromString(id))).thenReturn(Optional.of(comprovanteEntity()));

        ComprovanteConsultaResponse resultado = consultaService.consultar(id);

        assertEquals(id, resultado.getIdentificadorComprovante());
        assertEquals("Giovanni Vicente", resultado.getNome());
        verify(valueOperations).set(eq(cacheKey), any(ComprovanteConsultaResponse.class), eq(Duration.ofMinutes(10)));
    }

    @Test
    void deveLancar404QuandoIdNaoForUuidValido() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("comprovante:nao-e-uuid")).thenReturn(null);

        assertThrows(ComprovanteNaoEncontradoException.class, () -> consultaService.consultar("nao-e-uuid"));
        verify(comprovanteRepository, never()).findById(any());
    }

    @Test
    void deveLancar404AposTresTentativasSemEncontrar() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(comprovanteRepository.findById(UUID.fromString(id))).thenReturn(Optional.empty());

        assertThrows(ComprovanteNaoEncontradoException.class, () -> consultaService.consultar(id));
        verify(comprovanteRepository, times(3)).findById(UUID.fromString(id));
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }
}
