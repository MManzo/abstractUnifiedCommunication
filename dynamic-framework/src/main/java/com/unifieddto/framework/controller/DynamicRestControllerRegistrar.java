package com.unifieddto.framework.controller;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UriTemplate;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

@Component
public class DynamicRestControllerRegistrar implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DynamicRestControllerRegistrar.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private final ApplicationContext applicationContext;
    private final DynamicRestEndpoints dynamicEndpoints;
    private final JsonFormat.Printer jsonPrinter;
    private final JsonFormat.Parser jsonParser;

    public DynamicRestControllerRegistrar(RequestMappingHandlerMapping handlerMapping, ApplicationContext applicationContext, DynamicRestEndpoints dynamicEndpoints) {
        this.handlerMapping = handlerMapping;
        this.applicationContext = applicationContext;
        this.dynamicEndpoints = dynamicEndpoints;
        this.jsonPrinter = JsonFormat.printer().preservingProtoFieldNames();
        this.jsonParser = JsonFormat.parser().ignoringUnknownFields();
    }

    @Override
    public void afterPropertiesSet() {
        dynamicEndpoints.getEndpoints().forEach((name, props) -> {
            log.info("Registering dynamic REST endpoint: {}", name);
            try {
                register(props);
            } catch (Exception e) {
                log.error("Failed to register dynamic REST endpoint: {}", name, e);
            }
        });
    }

    private void register(DynamicRestEndpoints.EndpointProperties props) throws Exception {
        Object serviceBean = applicationContext.getBean(props.getServiceBeanName());

        // In POST/PUT, we can infer the DTO type from the service method signature
        Class<?> dtoClass;
        if (props.getDtoClassName() != null) {
            dtoClass = Class.forName(props.getDtoClassName());
        } else {
            dtoClass = findDtoClassForPost(serviceBean, props);
        }

        GenericRestHandler handler = new GenericRestHandler(serviceBean, dtoClass, props.getMappings());
        HandlerMethod handlerMethod = new HandlerMethod(handler, "handleRequest");

        RequestMappingInfo mappingInfo = RequestMappingInfo
                .paths(props.getPath())
                .methods(props.getHttpMethod())
                .build();

        handlerMapping.registerMapping(mappingInfo, handler, handlerMethod.getMethod());
        log.info("Mapped {} {} to {}.handleRequest", props.getHttpMethod(), props.getPath(), handler.getClass().getSimpleName());
    }

    private Class<?> findDtoClassForPost(Object serviceBean, DynamicRestEndpoints.EndpointProperties props) {
        for (Method method : serviceBean.getClass().getMethods()) {
            if (method.getName().equals("execute") && method.getParameterCount() == 1) {
                // This is a simplified inference. A real framework might need more robust logic.
                return method.getParameterTypes()[0];
            }
        }
        throw new IllegalArgumentException("Could not infer DTO type for service " + props.getServiceBeanName());
    }

    public class GenericRestHandler {
        private final Object serviceBean;
        private final Class<?> dtoClass;
        private final java.util.Map<String, String> mappings;
        private final Method executeMethod;

        public GenericRestHandler(Object serviceBean, Class<?> dtoClass, java.util.Map<String, String> mappings) throws Exception {
            this.serviceBean = serviceBean;
            this.dtoClass = dtoClass;
            this.mappings = mappings;
            this.executeMethod = serviceBean.getClass().getMethod("execute", dtoClass);
        }

        public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
            Message.Builder dtoBuilder = (Message.Builder) dtoClass.getMethod("newBuilder").invoke(null);

            if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
                String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                if (!body.isEmpty()) {
                    jsonParser.merge(body, dtoBuilder);
                }
            }

            UriTemplate uriTemplate = new UriTemplate(request.getServletPath());
            java.util.Map<String, String> pathVariables = uriTemplate.match(request.getRequestURI());

            for (java.util.Map.Entry<String, String> entry : mappings.entrySet()) {
                String dtoFieldName = entry.getKey();
                String[] sourceParts = entry.getValue().split(":");
                String source = sourceParts[0];
                String sourceName = sourceParts[1];

                String value = null;
                if ("path".equals(source)) {
                    value = pathVariables.get(sourceName);
                } else if ("query".equals(source)) {
                    value = request.getParameter(sourceName);
                } else if ("header".equals(source)) {
                    value = request.getHeader(sourceName);
                }

                if (value != null) {
                    try {
                        Method setter = dtoBuilder.getClass().getMethod("set" + com.google.common.base.CaseFormat.LOWER_CAMEL.to(com.google.common.base.CaseFormat.UPPER_CAMEL, dtoFieldName), String.class);
                        setter.invoke(dtoBuilder, value);
                    } catch (NoSuchMethodException e) {
                         // Handle non-string types - simplified for this example
                        log.warn("Could not find String setter for field {}. Type conversion would be needed for a production system.", dtoFieldName);
                    }
                }
            }

            Message requestDto = dtoBuilder.build();
            Object result = executeMethod.invoke(serviceBean, requestDto);

            response.setContentType("application/json");
            response.getWriter().write(jsonPrinter.print((Message) result));
        }
    }
}
