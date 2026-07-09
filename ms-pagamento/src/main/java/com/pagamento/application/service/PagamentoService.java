package com.pagamento.application.service;

import com.pagamento.domain.entity.Fatura;
import com.pagamento.domain.entity.Pagamento;
import com.pagamento.domain.enums.StatusFatura;
import com.pagamento.dto.ComprovanteResponse;
import com.pagamento.dto.PagamentoRequest;
import com.pagamento.dto.PagamentoResponse;
import com.pagamento.infrastructure.client.ComprovanteClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagamentoService {

    private final FaturaPersistenceService faturaPersistenceService;
    private final ComprovanteClient comprovanteClient;

    public PagamentoResponse processarPagamento(PagamentoRequest request) {
        // 1. Persistir fatura como PENDENTE
        Fatura fatura = Fatura.builder()
                .nome(request.getNome())
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento())
                .valorTransacao(request.getValorTransacao())
                .status(StatusFatura.PENDENTE)
                .build();

        fatura = faturaPersistenceService.salvar(fatura);
        log.info("Fatura {} criada com status PENDENTE", fatura.getId());

        // 2. Chamar MS Comprovantes (fora da transação)
        try {
            ComprovanteResponse comprovanteResponse = comprovanteClient.solicitarComprovante(request);

            // 3. Criar Pagamento e atualizar fatura para COMPROVANTE_SOLICITADO
            Pagamento pagamento = faturaPersistenceService.criarPagamento(fatura, comprovanteResponse.getIdentificadorComprovante(), request);
            log.info("Pagamento {} criado. Fatura {} atualizada para COMPROVANTE_SOLICITADO", pagamento.getId(), fatura.getId());

            return PagamentoResponse.builder()
                    .faturaId(fatura.getId())
                    .status(StatusFatura.COMPROVANTE_SOLICITADO.name())
                    .identificadorComprovante(comprovanteResponse.getIdentificadorComprovante())
                    .dataHoraRequisicao(comprovanteResponse.getDataHoraRequisicao())
                    .build();

        } catch (Exception e) {
            // Compensação: falha na chamada HTTP
            faturaPersistenceService.marcarComoFalha(fatura);
            log.error("Falha ao solicitar comprovante. Fatura {} marcada como FALHA: {}", fatura.getId(), e.getMessage());

            return PagamentoResponse.builder()
                    .faturaId(fatura.getId())
                    .status(StatusFatura.FALHA.name())
                    .build();
        }
    }
}
