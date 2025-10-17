package com.inferno.catalog_service.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;

public class CheckBalanceLambda implements RequestHandler<SQSEvent, Void> {
    private final DynamoDbClient ddb = DynamoDbClient.create();
    private final SqsClient sqs = SqsClient.create();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final String paymentTable = System.getenv("PAYMENT_TABLE");
    private final String nextQueueUrl = System.getenv("TRANSACTION_QUEUE_URL");
    private final String cardApiBaseUrl = System.getenv("CARD_API_BASE_URL"); // del terraform

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            try {
                String body = msg.getBody();
                context.getLogger().log("Raw message: " + body);

                JsonNode node = mapper.readTree(body);
                String traceId = node.get("traceId").asText();

                // Buscar el pago en DynamoDB
                Map<String, AttributeValue> pk = Map.of("traceId", AttributeValue.builder().s(traceId).build());
                GetItemResponse g = ddb.getItem(GetItemRequest.builder().tableName(paymentTable).key(pk).build());
                if (!g.hasItem()) {
                    context.getLogger().log("No payment record for trace " + traceId);
                    continue;
                }

                Map<String, AttributeValue> item = g.item();
                String cardId = item.get("cardId").s();

                // 🔥 Leer el campo "serviceData" (es un JSON en string)
                long amount = 0;
                if (item.containsKey("serviceData")) {
                    String serviceJson = item.get("serviceData").s();
                    JsonNode serviceNode = mapper.readTree(serviceJson);

                    context.getLogger().log("Service data from Dynamo: " + serviceJson);

                    if (serviceNode.has("precioMensual")) {
                        amount = serviceNode.get("precioMensual").asLong();
                    } else {
                        context.getLogger().log("Service data missing 'precioMensual' field for trace " + traceId);
                        markFailed(traceId, "Missing precioMensual field");
                        continue;
                    }
                } else {
                    context.getLogger().log("Item missing 'serviceData' field for trace " + traceId);
                    markFailed(traceId, "Missing serviceData field");
                    continue;
                }

                // Llamar al API Gateway para obtener info de la tarjeta
                String url = cardApiBaseUrl + "/card/profile/" + cardId;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    context.getLogger().log("Card API returned status " + response.statusCode());
                    markFailed(traceId, "Card not found or API error");
                    continue;
                }

                JsonNode cardInfo = mapper.readTree(response.body());
                context.getLogger().log("Card API response: " + response.body());

                if (!cardInfo.isArray() || cardInfo.isEmpty()) {
                    context.getLogger().log("Card API response is empty or not an array: " + response.body());
                    markFailed(traceId, "Invalid Card API response");
                    continue;
                }

                JsonNode card = cardInfo.get(0);

                // Verificar balance o límite
                String type = card.path("type").asText();
                long balance = card.path("balance").asLong(0);
                long limit = card.path("limit").asLong(0);
                long usedBalance = card.path("usedBalance").asLong(0);

                boolean ok;
                if ("DEBIT".equalsIgnoreCase(type)) {
                    ok = balance >= amount;
                } else {
                    ok = (usedBalance + amount) <= limit;
                }

                if (!ok) {
                    markFailed(traceId, "Insufficient funds");
                    continue;
                }

                // Actualizar a IN_PROGRESS y enviar siguiente evento
                ddb.updateItem(UpdateItemRequest.builder()
                        .tableName(paymentTable)
                        .key(pk)
                        .updateExpression("SET #s = :st, updatedAt = :now")
                        .expressionAttributeNames(Map.of("#s", "status"))
                        .expressionAttributeValues(Map.of(
                                ":st", AttributeValue.builder().s("IN_PROGRESS").build(),
                                ":now", AttributeValue.builder().s(Instant.now().toString()).build()
                        ))
                        .build());

                sqs.sendMessage(SendMessageRequest.builder()
                        .queueUrl(nextQueueUrl)
                        .messageBody("{\"traceId\":\"" + traceId + "\"}")
                        .build());

                context.getLogger().log("Payment " + traceId + " moved to IN_PROGRESS");

            } catch (Exception e) {
                context.getLogger().log("Error processing message: " + e.getMessage());
            }
        }
        return null;
    }

    private void markFailed(String traceId, String error) {
        ddb.updateItem(UpdateItemRequest.builder()
                .tableName(paymentTable)
                .key(Map.of("traceId", AttributeValue.builder().s(traceId).build()))
                .updateExpression("SET #s = :st, #e = :err, updatedAt = :now")
                .expressionAttributeNames(Map.of("#s", "status", "#e", "error"))
                .expressionAttributeValues(Map.of(
                        ":st", AttributeValue.builder().s("FAILED").build(),
                        ":err", AttributeValue.builder().s(error).build(),
                        ":now", AttributeValue.builder().s(Instant.now().toString()).build()
                ))
                .build());
    }
}
