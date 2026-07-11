package com.notificacao.domain.entity;

import com.notificacao.domain.enums.StatusNotificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificacao")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacao {

    @Id
    private UUID id;

    private UUID identificadorComprovante;

    private String nomeDestinatario;

    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    private StatusNotificacao status;

    // Garante que uma nova instância criada pelo builder comece com 0 quando o valor não for informado. Depois, o serviço deverá atualizar esse campo conforme o processamento real.
    @Builder.Default
    @Column(nullable = false)
    private Integer tentativas = 0;

    private LocalDateTime dataEnvio;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}