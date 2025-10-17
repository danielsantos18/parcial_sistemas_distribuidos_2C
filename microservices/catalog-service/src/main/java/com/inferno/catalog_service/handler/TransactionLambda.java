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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;

public class TransactionLambda implements RequestHandler<SQSEvent, Void> {

    private final DynamoDbClient ddb = DynamoDbClient.create();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final String paymentTable = System.getenv("PAYMENT_TABLE");
    private final String transactionApiBaseUrl = System.getenv("TRANSACTION_API_BASE_URL");

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        context.getLogger().log("Running TransactionLambda...\n");

        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            try {
                JsonNode node = mapper.readTree(msg.getBody());
                String traceId = node.get("traceId").asText();
                context.getLogger().log("Processing transaction for traceId: " + traceId + "\n");

                // Buscar el registro del pago en DynamoDB
                Map<String, AttributeValue> key = Map.of("traceId", AttributeValue.builder().s(traceId).build());
                GetItemResponse response = ddb.getItem(GetItemRequest.builder()
                        .tableName(paymentTable)
                        .key(key)
                        .build());

                if (!response.hasItem()) {
                    context.getLogger().log("Payment not found in table for traceId: " + traceId + "\n");
                    continue;
                }

                Map<String, AttributeValue> item = response.item();

                // Extraer datos necesarios
                String cardId = item.get("cardId").s();
                String serviceJson = item.get("serviceData").s();
                JsonNode serviceNode = mapper.readTree(serviceJson);
                String merchant = serviceNode.get("proveedor").asText();
                Double amount = serviceNode.get("precioMensual").asDouble();

                // Crear JSON del cuerpo de la transacción
                String transactionBody = mapper.createObjectNode()
                        .put("cardId", cardId)
                        .put("merchant", merchant)
                        .put("amount", amount)
                        .toString();

                context.getLogger().log("Sending transaction: " + transactionBody + "\n");

                // Enviar al API de transacciones
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(transactionApiBaseUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(transactionBody))
                        .build();

                HttpResponse<String> txResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                context.getLogger().log("Transaction API response: " + txResponse.body() + "\n");

            } catch (Exception e) {
                context.getLogger().log("Error processing transaction: " + e.getMessage() + "\n");
            }
        }

        return null;
    }
}