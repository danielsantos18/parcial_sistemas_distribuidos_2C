package com.inferno.catalog_service.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inferno.catalog_service.dto.StartPaymentRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class StartPaymentLambda implements RequestHandler<SQSEvent, Void> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String checkBalanceQueueUrl = System.getenv("CHECK_BALANCE_QUEUE_URL");
    private final String paymentTable = System.getenv("PAYMENT_TABLE");
    private final String cardApiBaseUrl = System.getenv("CARD_API_BASE_URL");
    private final SqsClient sqsClient = SqsClient.create();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            try {
                StartPaymentRequest request = mapper.readValue(msg.getBody(), StartPaymentRequest.class);
                context.getLogger().log("Processing payment for card: " + request.getCardId());

                // ===== 1. Obtener datos de la tarjeta desde el API =====
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(cardApiBaseUrl + request.getCardId()))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    context.getLogger().log("Card API error: " + response.statusCode());
                    continue;
                }

                JsonNode cardInfo = mapper.readTree(response.body());

                context.getLogger().log("Card API response: " + response.body());
                // Asegurar que la respuesta es un array y tomar el primer elemento
                if (!cardInfo.isArray() || cardInfo.isEmpty()) {
                    context.getLogger().log("Card API response is empty or not an array: " + response.body());
                    continue;
                }

                JsonNode card = cardInfo.get(0);
                String userId = card.path("userId").asText(null);

                if (userId == null) {
                    context.getLogger().log("Missing userId in card info: " + card.toString());
                    continue;
                }

                // ===== 2. Crear el registro en DynamoDB =====
                String traceId = UUID.randomUUID().toString();
                long timestamp = Instant.now().toEpochMilli();

                Map<String, AttributeValue> item = Map.of(
                        "traceId", AttributeValue.fromS(traceId),
                        "userId", AttributeValue.fromS(userId),
                        "cardId", AttributeValue.fromS(request.getCardId()),
                        "serviceId", AttributeValue.fromS(request.getService().getId()),
                        "serviceData", AttributeValue.fromS(mapper.writeValueAsString(request.getService())),
                        "status", AttributeValue.fromS("INITIAL"),
                        "timestamp", AttributeValue.fromN(String.valueOf(timestamp))
                );

                dynamoDb.putItem(PutItemRequest.builder()
                        .tableName(paymentTable)
                        .item(item)
                        .build());

                // ===== 3. Preparar el mensaje para la siguiente cola =====
                ObjectNode messageToNextStep = (ObjectNode) mapper.createObjectNode();
                messageToNextStep.put("traceId", traceId);
                messageToNextStep.put("userId", userId);
                messageToNextStep.put("cardId", request.getCardId());
                messageToNextStep.set("service", mapper.valueToTree(request.getService()));
                messageToNextStep.put("status", "INITIAL");
                messageToNextStep.put("timestamp", timestamp);
                context.getLogger().log("Service data: " + mapper.writeValueAsString(request.getService()));

                // ===== 4. Enviar a la cola de check-balance =====
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(checkBalanceQueueUrl)
                        .messageBody(mapper.writeValueAsString(messageToNextStep))
                        .build());

                context.getLogger().log("Payment request processed successfully: " + traceId);

            } catch (Exception e) {
                context.getLogger().log("Error processing message: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }
}

