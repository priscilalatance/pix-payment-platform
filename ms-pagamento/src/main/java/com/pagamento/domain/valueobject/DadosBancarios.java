package com.pagamento.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DadosBancarios {
    private String numeroAgencia;
    private String numeroConta;
    private String digitoVerificadorConta;
}
