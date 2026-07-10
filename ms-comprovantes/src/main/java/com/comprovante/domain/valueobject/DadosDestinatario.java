package com.comprovante.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DadosDestinatario {
    private String tipoChavePixDestino;
    private String chavePixDestino;
    private String nomeClienteDestino;
}
