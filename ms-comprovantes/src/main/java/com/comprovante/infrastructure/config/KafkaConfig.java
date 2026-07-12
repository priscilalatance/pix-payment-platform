package com.comprovante.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String PAGAMENTO_REALIZADO_TOPIC = "pagamento.realizado";

    @Bean
    public NewTopic pagamentoRealizadoTopic() {
        return TopicBuilder.name(PAGAMENTO_REALIZADO_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Producer com ObjectMapper configurado (JavaTimeModule + datas como ISO-8601,
     * nao como array de timestamp) para o payload de pagamento.realizado bater com o
     * contrato (projeto.md). Sem type headers (__TypeId__): o ms-notificacao mapeia
     * para o proprio DTO.
     */
    @Bean
    public ProducerFactory<String, Object> pagamentoProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pagamentoProducerFactory) {
        return new KafkaTemplate<>(pagamentoProducerFactory);
    }
}
