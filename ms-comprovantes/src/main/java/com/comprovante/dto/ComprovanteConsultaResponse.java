package com.comprovante.dto;

import com.comprovante.domain.entity.Comprovante;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Corpo do GET /comprovantes/{id}. Tambem e o objeto guardado no cache Redis,
 * por isso implementa Serializable e usa apenas tipos simples (sem proxies JPA).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprovanteConsultaResponse implements Serializable {

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

    public static ComprovanteConsultaResponse fromEntity(Comprovante c) {
        return ComprovanteConsultaResponse.builder()
                .identificadorComprovante(c.getId().toString())
                .nome(c.getNome())
                .tipoDocumento(c.getTipoDocumento())
                .numeroDocumento(c.getNumeroDocumento())
                .numeroAgencia(c.getNumeroAgencia())
                .numeroConta(c.getNumeroConta())
                .digitoVerificadorConta(c.getDigitoVerificadorConta())
                .valorTransacao(c.getDadosTransacao().getValorTransacao())
                .identificacaoPix(c.getDadosTransacao().getIdentificacaoPix())
                .dataHoraTransacao(c.getDadosTransacao().getDataHoraTransacao())
                .tipoChavePixDestino(c.getDadosDestinatario().getTipoChavePixDestino())
                .chavePixDestino(c.getDadosDestinatario().getChavePixDestino())
                .nomeClienteDestino(c.getDadosDestinatario().getNomeClienteDestino())
                .build();
    }
}
