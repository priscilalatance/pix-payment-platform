package com.pagamento.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoRequest {

    @NotBlank
    private String nome;

    @NotBlank
    @JsonProperty("tipo_documento")
    private String tipoDocumento;

    @NotBlank
    @JsonProperty("numero_documento")
    private String numeroDocumento;

    @NotBlank
    @JsonProperty("numero_agencia")
    private String numeroAgencia;

    @NotBlank
    @JsonProperty("numero_conta")
    private String numeroConta;

    @NotBlank
    @JsonProperty("digito_verificador_conta")
    private String digitoVerificadorConta;

    @NotNull
    @JsonProperty("valor_transacao")
    private BigDecimal valorTransacao;

    @NotBlank
    @JsonProperty("tipo_chave_pix_destino")
    private String tipoChavePixDestino;

    @NotBlank
    @JsonProperty("chave_pix_destino")
    private String chavePixDestino;

    @NotBlank
    @JsonProperty("nome_cliente_destino")
    private String nomeClienteDestino;

    @JsonProperty("identificacao_pix")
    private String identificacaoPix;

    @NotNull
    @JsonProperty("data_hora_transacao")
    private LocalDateTime dataHoraTransacao;
}
