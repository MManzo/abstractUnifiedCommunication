package com.unifieddto.framework.hexagonal.adapters;

import com.unifieddto.framework.hexagonal.domain.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for use cases that allows the dynamic framework to discover and execute them.
 * This component acts as a bridge between the Spring application context and the hexagonal architecture.
 */
@Component
public class UseCaseRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(UseCaseRegistry.class);
    
    private final ApplicationContext applicationContext;
    private final Map<String, Class<?>> requestTypeToUseCaseMap = new HashMap<>();
    
    public UseCaseRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        discoverUseCases();
    }
    
    /**
     * Finds a use case that can handle the given request type.
     * 
     * @param requestType The class of the request object
     * @return An optional containing the use case if found
     */
    @SuppressWarnings("unchecked")
    public <REQUEST, RESPONSE> Optional<UseCase<REQUEST, RESPONSE>> findUseCaseForRequest(Class<REQUEST> requestType) {
        Class<?> useCaseClass = requestTypeToUseCaseMap.get(requestType.getName());
        if (useCaseClass != null) {
            try {
                return Optional.of((UseCase<REQUEST, RESPONSE>) applicationContext.getBean(useCaseClass));
            } catch (Exception e) {
                // Log error and return empty
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
    
    /**
     * Registers a use case for a specific request type.
     * This method can be used to manually register use cases if needed.
     * 
     * @param requestType The request type class
     * @param useCaseClass The use case class
     */
    public void registerUseCase(Class<?> requestType, Class<? extends UseCase<?, ?>> useCaseClass) {
        requestTypeToUseCaseMap.put(requestType.getName(), useCaseClass);
    }
    
    /**
     * Discovers all use cases in the application context by examining their generic types.
     * This is a simplified implementation - in a production system you might use
     * annotations or other mechanisms for more precise discovery.
     */
    private void discoverUseCases() {
        Map<String, UseCase> useCases = applicationContext.getBeansOfType(UseCase.class);
        
        for (UseCase<?, ?> useCase : useCases.values()) {
            // Manual registration for known use cases
            // In a real implementation, you would use reflection to determine the request type
            // from the generic parameters of the UseCase interface
            String useCaseClassName = useCase.getClass().getSimpleName();
            
            // Register known use cases based on naming convention
            if (useCaseClassName.equals("CreateUserUseCase")) {
                try {
                    Class<?> requestType = Class.forName("com.unifieddto.api.user.CreateUserRequest");
                    @SuppressWarnings("unchecked")
                    Class<? extends UseCase<?,?>> useCaseClass = (Class<? extends UseCase<?,?>>) useCase.getClass();
                    registerUseCase(requestType, useCaseClass);
                } catch (ClassNotFoundException e) {
                    log.warn("Could not find request type for CreateUserUseCase", e);
                }
            } else if (useCaseClassName.equals("GetUserUseCase")) {
                try {
                    Class<?> requestType = Class.forName("com.unifieddto.api.user.GetUserRequest");
                    @SuppressWarnings("unchecked")
                    Class<? extends UseCase<?,?>> useCaseClass = (Class<? extends UseCase<?,?>>) useCase.getClass();
                    registerUseCase(requestType, useCaseClass);
                } catch (ClassNotFoundException e) {
                    log.warn("Could not find request type for GetUserUseCase", e);
                }
            }
        }
        
        log.info("Discovered {} use cases", requestTypeToUseCaseMap.size());
    }
    
    /**
     * Gets all registered use cases.
     * 
     * @return A map of request type names to use case classes
     */
    public Map<String, Class<?>> getAllRegisteredUseCases() {
        return new HashMap<>(requestTypeToUseCaseMap);
    }
}