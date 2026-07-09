package com.pagamento.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SagaCallbackMessage {

    @JsonProperty("identificador_comprovante")
    private String identificadorComprovante;

    private String status;

    private String motivo;

    @JsonProperty("data_hora_processamento")
    private LocalDateTime dataHoraProcessamento;
}
