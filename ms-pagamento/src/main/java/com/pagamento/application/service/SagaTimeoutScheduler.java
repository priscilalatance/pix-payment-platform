package com.pagamento.application.service;

import com.pagamento.domain.entity.Fatura;
import com.pagamento.domain.enums.StatusFatura;
import com.pagamento.domain.repository.FaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaTimeoutScheduler {

    private final FaturaRepository faturaRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void verificarFaturasExpiradas() {
        List<Fatura> expiradas = faturaRepository.findByStatusAndTimeoutEmBefore(
                StatusFatura.COMPROVANTE_SOLICITADO, LocalDateTime.now());

        for (Fatura fatura : expiradas) {
            fatura.setStatus(StatusFatura.FALHA);
            faturaRepository.save(fatura);
            log.warn("SAGA timeout - Fatura {} marcada como FALHA (sem callback em 30 minutos)", fatura.getId());
        }

        if (!expiradas.isEmpty()) {
            log.info("SAGA timeout: {} fatura(s) expirada(s) compensada(s)", expiradas.size());
        }

        // Faturas PENDENTE órfãs (criadas há mais de 5 minutos sem progresso)
        List<Fatura> orfas = faturaRepository.findByStatusAndCriadoEmBefore(
                StatusFatura.PENDENTE, LocalDateTime.now().minusMinutes(5));

        for (Fatura fatura : orfas) {
            fatura.setStatus(StatusFatura.FALHA);
            faturaRepository.save(fatura);
            log.warn("Fatura órfã {} marcada como FALHA (PENDENTE há mais de 5 minutos)", fatura.getId());
        }
    }
}
