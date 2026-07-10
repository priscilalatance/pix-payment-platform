package com.comprovante.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload enfileirado em comprovante.gerar: o corpo do POST + o identificador
 * (UUID) gerado no momento da requisicao. Consumido pelo ComprovanteQueueListener.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprovanteMessage {

    @JsonProperty("identificador_comprovante")
    private String identificadorComprovante;

    private String nome;

    @JsonProperty("tipo_documento")
    private String tipoDocumento;

    @JsonProperty("numero_documento")
    private String numeroDocumento;

    @JsonProperty("numero_agencia")
    private String numeroAgencia;

    @JsonProperty("numero_conta")
    private String numeroConta;

    @JsonProperty("digito_verificador_conta")
    private String digitoVerificadorConta;

    @JsonProperty("valor_transacao")
    private BigDecimal valorTransacao;

    @JsonProperty("tipo_chave_pix_destino")
    private String tipoChavePixDestino;

    @JsonProperty("chave_pix_destino")
    private String chavePixDestino;

    @JsonProperty("nome_cliente_destino")
    private String nomeClienteDestino;

    @JsonProperty("identificacao_pix")
    private String identificacaoPix;

    @JsonProperty("data_hora_transacao")
    private LocalDateTime dataHoraTransacao;
}
