package com.comprovante.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

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
}
