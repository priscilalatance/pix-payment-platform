package com.pagamento.domain.repository;

import com.pagamento.domain.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
    Optional<Pagamento> findByIdentificadorComprovante(String identificadorComprovante);
}
