package com.notificacao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagamentoRealizadoEvent(

        @JsonProperty("identificador_comprovante")
        UUID identificadorComprovante,

        @JsonProperty("nome")
        String nome,

        @JsonProperty("numero_documento")
        String numeroDocumento,

        @JsonProperty("valor_transacao")
        BigDecimal valorTransacao,

        @JsonProperty("chave_pix_destino")
        String chavePixDestino,

        @JsonProperty("nome_cliente_destino")
        String nomeClienteDestino,

        @JsonProperty("data_hora_transacao")
        LocalDateTime dataHoraTransacao
) {
}