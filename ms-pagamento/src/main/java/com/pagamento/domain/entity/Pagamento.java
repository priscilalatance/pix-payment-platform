package com.pagamento.domain.entity;

import com.pagamento.domain.valueobject.ChavePix;
import com.pagamento.domain.valueobject.DadosBancarios;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagamentos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fatura_id", nullable = false)
    private Fatura fatura;

    private String identificadorComprovante;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "numeroAgencia", column = @Column(name = "numero_agencia")),
            @AttributeOverride(name = "numeroConta", column = @Column(name = "numero_conta")),
            @AttributeOverride(name = "digitoVerificadorConta", column = @Column(name = "digito_verificador_conta"))
    })
    private DadosBancarios dadosBancarios;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "tipoChavePixDestino", column = @Column(name = "tipo_chave_pix_destino")),
            @AttributeOverride(name = "chavePixDestino", column = @Column(name = "chave_pix_destino"))
    })
    private ChavePix chavePix;

    private String nomeClienteDestino;
    private String identificacaoPix;
    private LocalDateTime dataHoraTransacao;
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}
