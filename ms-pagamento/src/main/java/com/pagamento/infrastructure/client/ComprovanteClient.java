package com.pagamento.infrastructure.client;

import com.pagamento.dto.ComprovanteResponse;
import com.pagamento.dto.PagamentoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComprovanteClient {

    private final RestTemplate restTemplate;

    @Value("${app.comprovantes.url}")
    private String comprovantesUrl;

    public ComprovanteResponse solicitarComprovante(PagamentoRequest request) {
        log.info("Chamando MS Comprovantes: POST {}", comprovantesUrl);
        ResponseEntity<ComprovanteResponse> response = restTemplate.postForEntity(
                comprovantesUrl, request, ComprovanteResponse.class);

        if (response.getBody() == null) {
            throw new RuntimeException("MS Comprovantes retornou resposta sem body");
        }

        return response.getBody();
    }
}
