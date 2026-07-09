package com.pagamento.domain.repository;

import com.pagamento.domain.entity.Fatura;
import com.pagamento.domain.enums.StatusFatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FaturaRepository extends JpaRepository<Fatura, UUID> {
    List<Fatura> findByStatusAndTimeoutEmBefore(StatusFatura status, LocalDateTime timeout);
    List<Fatura> findByStatusAndCriadoEmBefore(StatusFatura status, LocalDateTime criadoEm);
}
