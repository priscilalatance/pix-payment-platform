package com.notificacao.application.service;

import com.notificacao.domain.entity.Notificacao;
import com.notificacao.domain.enums.StatusNotificacao;
import com.notificacao.domain.repository.NotificacaoRepository;
import com.notificacao.dto.PagamentoRealizadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public void processar(PagamentoRealizadoEvent evento) {
        // Idempotencia: protege contra redelivery/retry do Kafka
        if (notificacaoRepository.existsByIdentificadorComprovante(evento.identificadorComprovante())) {
            log.warn("Notificacao do comprovante {} ja processada. Ignorando mensagem duplicada.",
                    evento.identificadorComprovante());
            return;
        }

        Notificacao notificacao = Notificacao.builder()
                .identificadorComprovante(evento.identificadorComprovante())
                .nomeDestinatario(evento.nome())
                .valor(evento.valorTransacao())
                .status(StatusNotificacao.ENVIADA)
                .tentativas(1)
                .dataEnvio(LocalDateTime.now())
                .build();


        notificacaoRepository.save(notificacao);

        log.info("Notificacao enviada para {} — PIX de R$ {} realizado com sucesso",
        evento.nome(),
        evento.valorTransacao());
    }

    public void registrarFalha(PagamentoRealizadoEvent evento, int tentativas) {
        Notificacao notificacao = Notificacao.builder()
                .identificadorComprovante(evento.identificadorComprovante())
                .nomeDestinatario(evento.nome())
                .valor(evento.valorTransacao())
                .status(StatusNotificacao.FALHA)
                .tentativas(tentativas)
                .dataEnvio(null)
                .build();

        notificacaoRepository.save(notificacao);

        log.error("Falha ao enviar notificacao para {} apos {} tentativas",
                evento.nome(),
                tentativas);
    }
}
