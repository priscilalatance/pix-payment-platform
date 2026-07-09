package com.pagamento.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SAGA_EXCHANGE = "saga.exchange";
    public static final String SAGA_SUCESSO_QUEUE = "saga.comprovante.sucesso";
    public static final String SAGA_FALHA_QUEUE = "saga.comprovante.falha";
    public static final String SAGA_SUCESSO_ROUTING_KEY = "saga.sucesso.routing-key";
    public static final String SAGA_FALHA_ROUTING_KEY = "saga.falha.routing-key";

    @Bean
    public DirectExchange sagaExchange() {
        return new DirectExchange(SAGA_EXCHANGE);
    }

    @Bean
    public Queue sagaSucessoQueue() {
        return QueueBuilder.durable(SAGA_SUCESSO_QUEUE).build();
    }

    @Bean
    public Queue sagaFalhaQueue() {
        return QueueBuilder.durable(SAGA_FALHA_QUEUE).build();
    }

    @Bean
    public Binding sagaSucessoBinding(Queue sagaSucessoQueue, DirectExchange sagaExchange) {
        return BindingBuilder.bind(sagaSucessoQueue).to(sagaExchange).with(SAGA_SUCESSO_ROUTING_KEY);
    }

    @Bean
    public Binding sagaFalhaBinding(Queue sagaFalhaQueue, DirectExchange sagaExchange) {
        return BindingBuilder.bind(sagaFalhaQueue).to(sagaExchange).with(SAGA_FALHA_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
