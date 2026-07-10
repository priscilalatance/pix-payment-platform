package com.comprovante.application.service;

import com.comprovante.dto.ComprovanteMessage;
import com.comprovante.dto.ComprovanteRequest;
import com.comprovante.dto.ComprovanteResponse;
import com.comprovante.infrastructure.messaging.ComprovanteProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComprovanteService {

    private final ComprovanteProducer comprovanteProducer;

    public ComprovanteResponse solicitarComprovante(ComprovanteRequest request) {
        // 1. Gera o identificador (UUID v4) devolvido ao cliente
        String identificador = UUID.randomUUID().toString();
        LocalDateTime dataHoraRequisicao = LocalDateTime.now();

        // 2. Publica na fila para gravacao assincrona
        ComprovanteMessage message = ComprovanteMessage.builder()
                .identificadorComprovante(identificador)
                .nome(request.getNome())
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento())
                .numeroAgencia(request.getNumeroAgencia())
                .numeroConta(request.getNumeroConta())
                .digitoVerificadorConta(request.getDigitoVerificadorConta())
                .valorTransacao(request.getValorTransacao())
                .tipoChavePixDestino(request.getTipoChavePixDestino())
                .chavePixDestino(request.getChavePixDestino())
                .nomeClienteDestino(request.getNomeClienteDestino())
                .identificacaoPix(request.getIdentificacaoPix())
                .dataHoraTransacao(request.getDataHoraTransacao())
                .build();

        comprovanteProducer.publicar(message);
        log.info("Comprovante {} solicitado", identificador);

        // 3. Retorna 202 com o identificador
        return ComprovanteResponse.builder()
                .identificadorComprovante(identificador)
                .dataHoraRequisicao(dataHoraRequisicao)
                .build();
    }
}
