package com.inferno.catalog_service.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inferno.catalog_service.dto.StartPaymentRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

public class PaymentLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final SqsClient sqsClient = SqsClient.builder().build();
    private final String queueUrl = System.getenv("START_PAYMENT_QUEUE_URL");

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> response = new HashMap<>();

        try {
            String body = (String) event.get("body");
            StartPaymentRequest request = mapper.readValue(body, StartPaymentRequest.class);

            String message = mapper.writeValueAsString(request);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(message)
                    .build());

            // Construir respuesta correcta para API Gateway (AWS_PROXY)
            response.put("statusCode", 200);
            response.put("headers", Map.of("Content-Type", "application/json"));
            response.put("body", "{\"status\": \"Payment started successfully\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("statusCode", 500);
            response.put("headers", Map.of("Content-Type", "application/json"));
            response.put("body", "{\"error\": \"" + e.getMessage() + "\"}");
        }

        return response;
    }
}
