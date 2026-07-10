package com.comprovante.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Permite desligar o consumer em testes (sem broker). Em producao: true.
    @Value("${app.rabbitmq.listener.auto-startup:true}")
    private boolean listenerAutoStartup;

    // Fila interna de gravacao (producer no POST, consumer para gravar no banco)
    public static final String COMPROVANTE_EXCHANGE = "comprovante.exchange";
    public static final String COMPROVANTE_GERAR_QUEUE = "comprovante.gerar";
    public static final String COMPROVANTE_ROUTING_KEY = "comprovante.routing-key";

    // Callback da SAGA (producer -> consumido pelo ms-pagamento).
    // Constantes devem bater com o RabbitMQConfig do ms-pagamento.
    public static final String SAGA_EXCHANGE = "saga.exchange";
    public static final String SAGA_SUCESSO_QUEUE = "saga.comprovante.sucesso";
    public static final String SAGA_FALHA_QUEUE = "saga.comprovante.falha";
    public static final String SAGA_SUCESSO_ROUTING_KEY = "saga.sucesso.routing-key";
    public static final String SAGA_FALHA_ROUTING_KEY = "saga.falha.routing-key";

    // ---- comprovante.gerar ----
    @Bean
    public DirectExchange comprovanteExchange() {
        return new DirectExchange(COMPROVANTE_EXCHANGE);
    }

    @Bean
    public Queue comprovanteGerarQueue() {
        return QueueBuilder.durable(COMPROVANTE_GERAR_QUEUE).build();
    }

    @Bean
    public Binding comprovanteGerarBinding(Queue comprovanteGerarQueue, DirectExchange comprovanteExchange) {
        return BindingBuilder.bind(comprovanteGerarQueue).to(comprovanteExchange).with(COMPROVANTE_ROUTING_KEY);
    }

    // ---- saga.exchange (declarado tambem aqui; declaracoes sao idempotentes) ----
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

    // ---- infra ----
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAutoStartup(listenerAutoStartup);
        return factory;
    }
}
