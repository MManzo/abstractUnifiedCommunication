package com.unifieddto.framework.hexagonal.config;

import com.unifieddto.framework.hexagonal.adapters.ProtobufInputValidator;
import com.unifieddto.framework.hexagonal.adapters.UseCaseExecutor;
import com.unifieddto.framework.hexagonal.ports.InputValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the hexagonal architecture components.
 * This configuration provides default implementations that can be overridden by applications.
 */
@Configuration
public class HexagonalArchitectureConfiguration {
    
    /**
     * Provides a default input validator for Protocol Buffer messages.
     * Applications can override this by providing their own InputValidator bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public InputValidator inputValidator() {
        return new ProtobufInputValidator();
    }
    
    /**
     * Provides the use case executor that handles validation and error management.
     */
    @Bean
    public UseCaseExecutor useCaseExecutor(InputValidator inputValidator) {
        return new UseCaseExecutor(inputValidator);
    }
}