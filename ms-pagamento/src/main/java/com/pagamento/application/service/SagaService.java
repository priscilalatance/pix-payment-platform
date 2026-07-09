package com.pagamento.application.service;

import com.pagamento.domain.entity.Fatura;
import com.pagamento.domain.entity.Pagamento;
import com.pagamento.domain.enums.StatusFatura;
import com.pagamento.domain.repository.FaturaRepository;
import com.pagamento.domain.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaService {

    private final FaturaRepository faturaRepository;
    private final PagamentoRepository pagamentoRepository;

    @Transactional
    public void completarSaga(String identificadorComprovante) {
        Pagamento pagamento = pagamentoRepository.findByIdentificadorComprovante(identificadorComprovante)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado para comprovante: " + identificadorComprovante));

        Fatura fatura = pagamento.getFatura();

        if (fatura.getStatus() != StatusFatura.COMPROVANTE_SOLICITADO) {
            log.warn("Fatura {} já processada (status: {}). Ignorando callback de sucesso.", fatura.getId(), fatura.getStatus());
            return;
        }

        fatura.setStatus(StatusFatura.PAGA);
        fatura.setTimeoutEm(null);
        faturaRepository.save(fatura);
        log.info("Fatura {} marcada como PAGA", fatura.getId());
    }

    @Transactional
    public void compensarSaga(String identificadorComprovante, String motivo) {
        Pagamento pagamento = pagamentoRepository.findByIdentificadorComprovante(identificadorComprovante)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado para comprovante: " + identificadorComprovante));

        Fatura fatura = pagamento.getFatura();

        if (fatura.getStatus() != StatusFatura.COMPROVANTE_SOLICITADO) {
            log.warn("Fatura {} já processada (status: {}). Ignorando callback de falha.", fatura.getId(), fatura.getStatus());
            return;
        }

        fatura.setStatus(StatusFatura.FALHA);
        fatura.setTimeoutEm(null);
        faturaRepository.save(fatura);
        log.warn("SAGA compensada - Fatura {} marcada como FALHA. Motivo: {}", fatura.getId(), motivo);
    }
}
