package com.pagamento.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChavePix {
    private String tipoChavePixDestino;
    private String chavePixDestino;
}
