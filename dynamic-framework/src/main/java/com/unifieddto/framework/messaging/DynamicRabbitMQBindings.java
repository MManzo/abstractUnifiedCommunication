package com.unifieddto.framework.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.rabbitmq")
public class DynamicRabbitMQBindings {

    private final Map<String, BindingProperties> bindings = new HashMap<>();

    public Map<String, BindingProperties> getBindings() {
        return bindings;
    }

    public static class BindingProperties {
        private String queueName;
        private String exchangeName;
        private String routingKey;
        private String serviceBeanName;
        private String dtoClassName;

        // Getters and setters
        public String getQueueName() { return queueName; }
        public void setQueueName(String queueName) { this.queueName = queueName; }
        public String getExchangeName() { return exchangeName; }
        public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }
        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
        public String getServiceBeanName() { return serviceBeanName; }
        public void setServiceBeanName(String serviceBeanName) { this.serviceBeanName = serviceBeanName; }
        public String getDtoClassName() { return dtoClassName; }
        public void setDtoClassName(String dtoClassName) { this.dtoClassName = dtoClassName; }
    }
}
