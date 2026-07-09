package com.pagamento.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoResponse {

    @JsonProperty("fatura_id")
    private UUID faturaId;

    private String status;

    @JsonProperty("identificador_comprovante")
    private String identificadorComprovante;

    @JsonProperty("data_hora_requisicao")
    private LocalDateTime dataHoraRequisicao;
}
