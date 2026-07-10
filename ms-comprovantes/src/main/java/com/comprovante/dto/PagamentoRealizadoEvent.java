package com.comprovante.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento publicado no topico Kafka pagamento.realizado apos gravar o comprovante.
 * Consumido pelo ms-notificacao.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoRealizadoEvent {

    @JsonProperty("identificador_comprovante")
    private String identificadorComprovante;

    private String nome;

    @JsonProperty("numero_documento")
    private String numeroDocumento;

    @JsonProperty("valor_transacao")
    private BigDecimal valorTransacao;

    @JsonProperty("chave_pix_destino")
    private String chavePixDestino;

    @JsonProperty("nome_cliente_destino")
    private String nomeClienteDestino;

    @JsonProperty("data_hora_transacao")
    private LocalDateTime dataHoraTransacao;
}
