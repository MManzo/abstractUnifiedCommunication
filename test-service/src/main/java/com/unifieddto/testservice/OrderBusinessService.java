package com.unifieddto.testservice;

import com.unifieddto.api.order.CreateOrderRequest;
import com.unifieddto.api.order.CreateOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderBusinessService {

    private static final Logger log = LoggerFactory.getLogger(OrderBusinessService.class);

    /**
     * Business logic to create a new order.
     * This method will be dynamically exposed via REST and RabbitMQ by the framework.
     */
    public CreateOrderResponse execute(CreateOrderRequest request) {
        log.info("--- ORDER BUSINESS LOGIC ---");
        log.info("Executing logic for CreateOrderRequest for product {} and customer {}",
                request.getProductId(), request.getCustomerId());

        String newOrderId = "ORD-" + UUID.randomUUID().toString();

        log.info("New order created with ID: {}", newOrderId);

        return CreateOrderResponse.newBuilder()
                .setOrderId(newOrderId)
                .setStatusMessage("Order for product " + request.getProductId() + " has been accepted.")
                .build();
    }
}
