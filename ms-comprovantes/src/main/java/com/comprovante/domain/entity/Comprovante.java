package com.comprovante.domain.entity;

import com.comprovante.domain.valueobject.DadosDestinatario;
import com.comprovante.domain.valueobject.DadosTransacao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comprovante")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comprovante {

    /**
     * O identificador do comprovante (UUID v4) e gerado no POST /comprovantes
     * (ComprovanteService) e devolvido ao cliente antes da gravacao. O consumer
     * grava com esse mesmo id, por isso ele e atribuido explicitamente e NAO via
     * @GeneratedValue.
     */
    @Id
    private UUID id;

    private String nome;
    private String tipoDocumento;
    private String numeroDocumento;
    private String numeroAgencia;
    private String numeroConta;
    private String digitoVerificadorConta;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "valorTransacao", column = @Column(name = "valor_transacao")),
            @AttributeOverride(name = "identificacaoPix", column = @Column(name = "identificacao_pix")),
            @AttributeOverride(name = "dataHoraTransacao", column = @Column(name = "data_hora_transacao"))
    })
    private DadosTransacao dadosTransacao;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "tipoChavePixDestino", column = @Column(name = "tipo_chave_pix_destino")),
            @AttributeOverride(name = "chavePixDestino", column = @Column(name = "chave_pix_destino")),
            @AttributeOverride(name = "nomeClienteDestino", column = @Column(name = "nome_cliente_destino"))
    })
    private DadosDestinatario dadosDestinatario;

    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}
