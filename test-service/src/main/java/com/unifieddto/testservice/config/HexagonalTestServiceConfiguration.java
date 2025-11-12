package com.unifieddto.testservice.config;

import com.unifieddto.framework.hexagonal.config.HexagonalArchitectureConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class that enables the hexagonal architecture for the test service.
 * This imports the framework's hexagonal configuration and enables component scanning
 * for use cases and adapters.
 */
@Configuration
@Import(HexagonalArchitectureConfiguration.class)
public class HexagonalTestServiceConfiguration {
    
    // This configuration class serves as an entry point for the hexagonal architecture
    // All use cases and adapters will be automatically discovered by Spring's component scanning
}