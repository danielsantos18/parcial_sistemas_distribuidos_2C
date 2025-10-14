package com.inferno.card_service.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inferno.card_service.dto.CardPurchaseRequest;
import com.inferno.card_service.dto.TransactionResponse;
import com.inferno.card_service.model.Card;
import com.inferno.card_service.service.CardService;
import com.inferno.card_service.service.TransactionService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class CardPurchaseLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CardService cardService = new CardService();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionService transactionService = new TransactionService();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");

        try {
            // 🧩 Parsear body a DTO
            CardPurchaseRequest purchaseRequest = objectMapper.readValue(request.getBody(), CardPurchaseRequest.class);

            // ⚙️ Procesar compra
            TransactionResponse response = transactionService.purchase(purchaseRequest);

            // ✅ Respuesta exitosa
            String responseJson = objectMapper.writeValueAsString(response);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(responseJson);

        } catch (RuntimeException e) {
            // ⚠️ Errores de negocio (como "Insufficient funds")
            context.getLogger().log("⚠️ Business Error: " + e.getMessage() + "\n");

            Map<String, Object> errorBody = Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "details", e.getClass().getSimpleName()
            );

            try {
                String errorJson = objectMapper.writeValueAsString(errorBody);
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(headers)
                        .withBody(errorJson);
            } catch (Exception ex) {
                return createFallbackError(headers);
            }

        } catch (Exception e) {
            // ❌ Errores inesperados
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            context.getLogger().log("❌ Unexpected Error: " + sw + "\n");

            Map<String, Object> errorBody = Map.of(
                    "status", "error",
                    "message", "Internal Server Error",
                    "details", e.getClass().getSimpleName()
            );

            try {
                String errorJson = objectMapper.writeValueAsString(errorBody);
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(500)
                        .withHeaders(headers)
                        .withBody(errorJson);
            } catch (Exception ex) {
                return createFallbackError(headers);
            }
        }
    }

    private APIGatewayProxyResponseEvent createFallbackError(Map<String, String> headers) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(headers)
                .withBody("{\"error\": \"Internal Server Error\"}");
    }
}
