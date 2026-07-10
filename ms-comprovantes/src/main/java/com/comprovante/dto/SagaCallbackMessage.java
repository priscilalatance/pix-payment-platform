package com.comprovante.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Callback publicado no saga.exchange. Os campos devem bater exatamente com o
 * DTO consumido pelo ms-pagamento (SagaCallbackMessage), para garantir a
 * (de)serializacao entre os servicos.
 */
@Data
@Builder
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
