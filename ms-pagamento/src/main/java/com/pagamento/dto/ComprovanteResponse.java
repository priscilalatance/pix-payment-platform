package com.pagamento.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprovanteResponse {

    @JsonProperty("identificador_comprovante")
    private String identificadorComprovante;

    @JsonProperty("data_hora_requisicao")
    private LocalDateTime dataHoraRequisicao;
}
