package com.comprovante.application.service;

import com.comprovante.application.exception.ComprovanteNaoEncontradoException;
import com.comprovante.domain.entity.Comprovante;
import com.comprovante.domain.repository.ComprovanteRepository;
import com.comprovante.dto.ComprovanteConsultaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ComprovanteConsultaService {

    private static final String CACHE_PREFIX = "comprovante:";

    private final RedisTemplate<String, ComprovanteConsultaResponse> redisTemplate;
    private final ComprovanteRepository comprovanteRepository;
    private final int maxTentativas;
    private final long delayMs;
    private final long ttlMinutes;

    public ComprovanteConsultaService(
            RedisTemplate<String, ComprovanteConsultaResponse> redisTemplate,
            ComprovanteRepository comprovanteRepository,
            @Value("${app.consulta.max-tentativas:3}") int maxTentativas,
            @Value("${app.consulta.delay-ms:300}") long delayMs,
            @Value("${app.cache.comprovante.ttl-minutes:10}") long ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.comprovanteRepository = comprovanteRepository;
        this.maxTentativas = maxTentativas;
        this.delayMs = delayMs;
        this.ttlMinutes = ttlMinutes;
    }

    public ComprovanteConsultaResponse consultar(String id) {
        String cacheKey = CACHE_PREFIX + id;

        // 1. Cache Redis
        ComprovanteConsultaResponse cacheado = redisTemplate.opsForValue().get(cacheKey);
        if (cacheado != null) {
            log.info("Comprovante {} servido do cache Redis", id);
            return cacheado;
        }

        UUID uuid = parseUuid(id);

        // 2. Banco com ate N tentativas (cobre a gravacao assincrona)
        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            Optional<Comprovante> encontrado = comprovanteRepository.findById(uuid);
            if (encontrado.isPresent()) {
                ComprovanteConsultaResponse response = ComprovanteConsultaResponse.fromEntity(encontrado.get());
                redisTemplate.opsForValue().set(cacheKey, response, Duration.ofMinutes(ttlMinutes));
                log.info("Comprovante {} encontrado no banco (tentativa {}) e cacheado", id, tentativa);
                return response;
            }
            log.warn("Comprovante {} nao encontrado (tentativa {}/{})", id, tentativa, maxTentativas);
            aguardar(tentativa);
        }

        // 3. Nao encontrado apos as tentativas -> 404
        throw new ComprovanteNaoEncontradoException(id);
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ComprovanteNaoEncontradoException(id);
        }
    }

    private void aguardar(int tentativa) {
        if (tentativa >= maxTentativas) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
