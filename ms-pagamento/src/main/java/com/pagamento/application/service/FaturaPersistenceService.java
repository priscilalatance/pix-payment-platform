package com.pagamento.application.service;

import com.pagamento.domain.entity.Fatura;
import com.pagamento.domain.entity.Pagamento;
import com.pagamento.domain.enums.StatusFatura;
import com.pagamento.domain.repository.FaturaRepository;
import com.pagamento.domain.repository.PagamentoRepository;
import com.pagamento.domain.valueobject.ChavePix;
import com.pagamento.domain.valueobject.DadosBancarios;
import com.pagamento.dto.PagamentoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FaturaPersistenceService {

    private final FaturaRepository faturaRepository;
    private final PagamentoRepository pagamentoRepository;

    @Transactional
    public Fatura salvar(Fatura fatura) {
        return faturaRepository.save(fatura);
    }

    @Transactional
    public Pagamento criarPagamento(Fatura fatura, String identificadorComprovante, PagamentoRequest request) {
        fatura.setStatus(StatusFatura.COMPROVANTE_SOLICITADO);
        fatura.setTimeoutEm(LocalDateTime.now().plusMinutes(30));
        faturaRepository.save(fatura);

        Pagamento pagamento = Pagamento.builder()
                .fatura(fatura)
                .identificadorComprovante(identificadorComprovante)
                .dadosBancarios(new DadosBancarios(
                        request.getNumeroAgencia(),
                        request.getNumeroConta(),
                        request.getDigitoVerificadorConta()))
                .chavePix(new ChavePix(
                        request.getTipoChavePixDestino(),
                        request.getChavePixDestino()))
                .nomeClienteDestino(request.getNomeClienteDestino())
                .identificacaoPix(request.getIdentificacaoPix())
                .dataHoraTransacao(request.getDataHoraTransacao())
                .build();

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public void marcarComoFalha(Fatura fatura) {
        fatura.setStatus(StatusFatura.FALHA);
        faturaRepository.save(fatura);
    }
}
