package edu.udla.integracion.progreso2.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // --- Colas ---
    @Bean
    public Queue billingQueue() {
        return QueueBuilder.durable("billing.queue").build();
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable("notifications.queue").build();
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable("analytics.queue").build();
    }

    // --- Exchanges ---

    // Point-to-Point: exchange directo para facturación
    @Bean
    public DirectExchange billingExchange() {
        return new DirectExchange("billing", true, false);
    }

    // Publish/Subscribe: exchange fanout para eventos de cita confirmada
    @Bean
    public FanoutExchange appointmentsEventsExchange() {
        return new FanoutExchange("appointments.events", true, false);
    }

    // --- Bindings ---

    // billing.queue escucha en el exchange "billing" con routingKey "billing.queue"
    @Bean
    public Binding billingBinding(Queue billingQueue, DirectExchange billingExchange) {
        return BindingBuilder.bind(billingQueue).to(billingExchange).with("billing.queue");
    }

    // notifications.queue y analytics.queue reciben todos los mensajes del fanout
    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, FanoutExchange appointmentsEventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(appointmentsEventsExchange);
    }

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, FanoutExchange appointmentsEventsExchange) {
        return BindingBuilder.bind(analyticsQueue).to(appointmentsEventsExchange);
    }
}
