package org.multivix.pulsealertworker.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_PROCESSED_EVENTS = "processed_events";
    public static final String QUEUE_VITAL_SIGNALS = "vital_signals";

    @Bean
    public Queue processedEventsQueue() {
        return new Queue(QUEUE_PROCESSED_EVENTS, true);
    }

    // Garante que o Worker não quebre se subir antes do Gateway
    @Bean
    public Queue vitalSignalsQueue() {
        return new Queue(QUEUE_VITAL_SIGNALS, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}