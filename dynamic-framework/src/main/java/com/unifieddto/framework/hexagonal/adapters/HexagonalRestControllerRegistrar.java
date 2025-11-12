package com.unifieddto.framework.hexagonal.adapters;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.unifieddto.framework.controller.DynamicRestEndpoints;
import com.unifieddto.framework.hexagonal.domain.BusinessException;
import com.unifieddto.framework.hexagonal.domain.UseCase;
import com.unifieddto.framework.hexagonal.domain.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Hexagonal architecture version of the dynamic REST controller registrar.
 * This adapter bridges HTTP requests to use cases while maintaining proper separation of concerns.
 */
@Component
public class HexagonalRestControllerRegistrar implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(HexagonalRestControllerRegistrar.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private final DynamicRestEndpoints dynamicEndpoints;
    private final UseCaseRegistry useCaseRegistry;
    private final UseCaseExecutor useCaseExecutor;
    private final JsonFormat.Printer jsonPrinter;
    private final JsonFormat.Parser jsonParser;

    public HexagonalRestControllerRegistrar(RequestMappingHandlerMapping handlerMapping,
                                          DynamicRestEndpoints dynamicEndpoints,
                                          UseCaseRegistry useCaseRegistry,
                                          UseCaseExecutor useCaseExecutor) {
        this.handlerMapping = handlerMapping;
        this.dynamicEndpoints = dynamicEndpoints;
        this.useCaseRegistry = useCaseRegistry;
        this.useCaseExecutor = useCaseExecutor;
        this.jsonPrinter = JsonFormat.printer().preservingProtoFieldNames();
        this.jsonParser = JsonFormat.parser().ignoringUnknownFields();
    }

    @Override
    public void afterPropertiesSet() {
        dynamicEndpoints.getEndpoints().forEach((name, props) -> {
            log.info("Registering hexagonal REST endpoint: {}", name);
            try {
                register(props);
            } catch (Exception e) {
                log.error("Failed to register hexagonal REST endpoint: {}", name, e);
            }
        });
    }

    private void register(DynamicRestEndpoints.EndpointProperties props) throws Exception {
        Class<?> requestClass = Class.forName(props.getDtoClassName());
        
        HexagonalRestHandler handler = new HexagonalRestHandler(requestClass, props.getMappings());
        HandlerMethod handlerMethod = new HandlerMethod(handler, "handleRequest");

        RequestMappingInfo mappingInfo = RequestMappingInfo
                .paths(props.getPath())
                .methods(props.getHttpMethod())
                .build();

        handlerMapping.registerMapping(mappingInfo, handler, handlerMethod.getMethod());
        log.info("Mapped {} {} to hexagonal handler for request type: {}", 
                props.getHttpMethod(), props.getPath(), requestClass.getSimpleName());
    }

    public class HexagonalRestHandler {
        private final Class<?> requestClass;
        private final java.util.Map<String, String> mappings;

        public HexagonalRestHandler(Class<?> requestClass, java.util.Map<String, String> mappings) {
            this.requestClass = requestClass;
            this.mappings = mappings != null ? mappings : java.util.Map.of();
        }

        public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
            try {
                // Build the request DTO from HTTP request
                Message requestDto = buildRequestDto(request);
                
                // Find the appropriate use case
                @SuppressWarnings("unchecked")
                Optional<UseCase<Message, Message>> useCase = useCaseRegistry.findUseCaseForRequest((Class<Message>) requestClass);
                
                if (useCase.isEmpty()) {
                    sendErrorResponse(response, HttpStatus.NOT_IMPLEMENTED, 
                            "USE_CASE_NOT_FOUND", "No use case found for request type: " + requestClass.getSimpleName());
                    return;
                }
                
                // Execute the use case
                Message responseDto = useCaseExecutor.execute(useCase.get(), requestDto);
                
                // Send successful response
                sendSuccessResponse(response, responseDto);
                
            } catch (ValidationException e) {
                log.warn("Validation error in REST endpoint: {}", e.getMessage());
                sendValidationErrorResponse(response, e);
            } catch (BusinessException e) {
                log.warn("Business error in REST endpoint: {}", e.getMessage());
                sendBusinessErrorResponse(response, e);
            } catch (Exception e) {
                log.error("Unexpected error in REST endpoint", e);
                sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, 
                        "INTERNAL_ERROR", "An unexpected error occurred");
            }
        }

        private Message buildRequestDto(HttpServletRequest request) throws Exception {
            Message.Builder dtoBuilder = (Message.Builder) requestClass.getMethod("newBuilder").invoke(null);

            // Handle request body for POST/PUT
            if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
                String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                if (!body.isEmpty()) {
                    jsonParser.merge(body, dtoBuilder);
                }
            }

            // Handle path variables, query parameters, and headers
            UriTemplate uriTemplate = new UriTemplate(request.getServletPath());
            java.util.Map<String, String> pathVariables = uriTemplate.match(request.getRequestURI());

            for (java.util.Map.Entry<String, String> entry : mappings.entrySet()) {
                String dtoFieldName = entry.getKey();
                String[] sourceParts = entry.getValue().split(":");
                String source = sourceParts[0];
                String sourceName = sourceParts[1];

                String value = extractValue(request, pathVariables, source, sourceName);
                if (value != null) {
                    setFieldValue(dtoBuilder, dtoFieldName, value);
                }
            }

            return dtoBuilder.build();
        }

        private String extractValue(HttpServletRequest request, java.util.Map<String, String> pathVariables, 
                                  String source, String sourceName) {
            return switch (source) {
                case "path" -> pathVariables.get(sourceName);
                case "query" -> request.getParameter(sourceName);
                case "header" -> request.getHeader(sourceName);
                default -> null;
            };
        }

        private void setFieldValue(Message.Builder builder, String fieldName, String value) {
            try {
                Method setter = builder.getClass().getMethod("set" + 
                        com.google.common.base.CaseFormat.LOWER_CAMEL.to(
                                com.google.common.base.CaseFormat.UPPER_CAMEL, fieldName), String.class);
                setter.invoke(builder, value);
            } catch (Exception e) {
                log.warn("Could not set field {} with value {}: {}", fieldName, value, e.getMessage());
            }
        }

        private void sendSuccessResponse(HttpServletResponse response, Message responseDto) throws IOException {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/json");
            response.getWriter().write(jsonPrinter.print(responseDto));
        }

        private void sendValidationErrorResponse(HttpServletResponse response, ValidationException e) throws IOException {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType("application/json");
            
            StringBuilder errorJson = new StringBuilder();
            errorJson.append("{\"error\":\"VALIDATION_ERROR\",\"message\":\"").append(e.getMessage()).append("\",\"details\":[");
            
            for (int i = 0; i < e.getValidationErrors().size(); i++) {
                if (i > 0) errorJson.append(",");
                ValidationException.ValidationError error = e.getValidationErrors().get(i);
                errorJson.append("{\"field\":\"").append(error.getField())
                        .append("\",\"message\":\"").append(error.getMessage()).append("\"}");
            }
            
            errorJson.append("]}");
            response.getWriter().write(errorJson.toString());
        }

        private void sendBusinessErrorResponse(HttpServletResponse response, BusinessException e) throws IOException {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\":\"%s\",\"message\":\"%s\"}", 
                    e.getErrorCode(), e.getMessage()));
        }

        private void sendErrorResponse(HttpServletResponse response, HttpStatus status, 
                                     String errorCode, String message) throws IOException {
            response.setStatus(status.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\":\"%s\",\"message\":\"%s\"}", 
                    errorCode, message));
        }
    }
}