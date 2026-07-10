package com.comprovante.application.service;

import com.comprovante.domain.entity.Comprovante;
import com.comprovante.domain.repository.ComprovanteRepository;
import com.comprovante.domain.valueobject.DadosDestinatario;
import com.comprovante.domain.valueobject.DadosTransacao;
import com.comprovante.dto.ComprovanteMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ComprovantePersistenceService {

    private final ComprovanteRepository comprovanteRepository;

    public boolean existe(UUID id) {
        return comprovanteRepository.existsById(id);
    }

    @Transactional
    public Comprovante gravar(ComprovanteMessage message) {
        Comprovante comprovante = Comprovante.builder()
                .id(UUID.fromString(message.getIdentificadorComprovante()))
                .nome(message.getNome())
                .tipoDocumento(message.getTipoDocumento())
                .numeroDocumento(message.getNumeroDocumento())
                .numeroAgencia(message.getNumeroAgencia())
                .numeroConta(message.getNumeroConta())
                .digitoVerificadorConta(message.getDigitoVerificadorConta())
                .dadosTransacao(new DadosTransacao(
                        message.getValorTransacao(),
                        message.getIdentificacaoPix(),
                        message.getDataHoraTransacao()))
                .dadosDestinatario(new DadosDestinatario(
                        message.getTipoChavePixDestino(),
                        message.getChavePixDestino(),
                        message.getNomeClienteDestino()))
                .build();

        return comprovanteRepository.save(comprovante);
    }
}
