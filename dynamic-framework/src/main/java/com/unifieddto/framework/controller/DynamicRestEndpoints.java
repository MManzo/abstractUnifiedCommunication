package com.unifieddto.framework.controller;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.rest")
public class DynamicRestEndpoints {

    private final Map<String, EndpointProperties> endpoints = new HashMap<>();

    public Map<String, EndpointProperties> getEndpoints() {
        return endpoints;
    }

    public static class EndpointProperties {
        private String path;
        private RequestMethod httpMethod;
        private String serviceBeanName;
        private String dtoClassName;
        private Map<String, String> mappings = new HashMap<>();

        // Getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public RequestMethod getHttpMethod() { return httpMethod; }
        public void setHttpMethod(RequestMethod httpMethod) { this.httpMethod = httpMethod; }
        public String getServiceBeanName() { return serviceBeanName; }
        public void setServiceBeanName(String serviceBeanName) { this.serviceBeanName = serviceBeanName; }
        public String getDtoClassName() { return dtoClassName; }
        public void setDtoClassName(String dtoClassName) { this.dtoClassName = dtoClassName; }
        public Map<String, String> getMappings() { return mappings; }
        public void setMappings(Map<String, String> mappings) { this.mappings = mappings; }
    }
}
