package com.comprovante.domain.repository;

import com.comprovante.domain.entity.Comprovante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComprovanteRepository extends JpaRepository<Comprovante, UUID> {
}
