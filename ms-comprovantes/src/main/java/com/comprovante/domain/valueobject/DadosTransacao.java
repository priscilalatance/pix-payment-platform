package com.comprovante.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DadosTransacao {
    private BigDecimal valorTransacao;
    private String identificacaoPix;
    private LocalDateTime dataHoraTransacao;
}
