package com.pagamento.domain.entity;

import com.pagamento.domain.enums.StatusFatura;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "faturas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fatura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private String tipoDocumento;
    private String numeroDocumento;
    private BigDecimal valorTransacao;

    @Enumerated(EnumType.STRING)
    private StatusFatura status;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private LocalDateTime timeoutEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        if (this.status == null) {
            this.status = StatusFatura.PENDENTE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
