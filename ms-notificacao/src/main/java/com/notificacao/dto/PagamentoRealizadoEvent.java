package com.notificacao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagamentoRealizadoEvent(
        UUID identificadorComprovante,
        String nome,
        String numeroDocumento,
        BigDecimal valorTransacao,
        String chavePixDestino,
        String nomeClienteDestino,
        LocalDateTime dataHoraTransacao
) {
}