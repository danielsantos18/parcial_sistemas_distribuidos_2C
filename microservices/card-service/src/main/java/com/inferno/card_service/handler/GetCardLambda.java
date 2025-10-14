package com.inferno.card_service.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inferno.card_service.dto.CardResponse;
import com.inferno.card_service.model.Card;
import com.inferno.card_service.service.CardService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetCardLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final CardService cardService = new CardService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> headers = createHeaders();

        try {
            // Obtener UUID del path
            String uuid = request.getPathParameters().get("card_id");
            if (uuid == null || uuid.isBlank()) {
                return createErrorResponse(400, "Card UUID is required in path", headers);
            }

            // Llamar al servicio
            List<Card> card = cardService.getCardsByUuid(uuid);

            if (card == null || card.isEmpty()) {
                return createErrorResponse(404, "Card not found", headers);
            }

            // Mapear a DTO
            List<CardResponse> responses = card.stream()
                    .map(this::createCardResponse)
                    .collect(Collectors.toList());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(objectMapper.writeValueAsString(responses));

        } catch (Exception e) {
            context.getLogger().log("Get user error: " + e.getMessage());
            return createErrorResponse(500, "Internal server error", headers);
        }
    }

    private CardResponse createCardResponse(Card card) {
        CardResponse response = new CardResponse();
        response.setUuid(card.getUuid());
        response.setUserId(card.getUserId());
        response.setType(card.getType());
        response.setStatus(card.getStatus());
        response.setBalance(card.getBalance());
        response.setLimit(card.getLimit());
        response.setUsedBalance(card.getUsedBalance());
        response.setScore(card.getScore());
        response.setCreatedAt(card.getCreatedAt());
        return response;
    }

    private Map<String, String> createHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        return headers;
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message, Map<String, String> headers) {
        try {
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", message);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(headers)
                    .withBody(objectMapper.writeValueAsString(errorBody));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(headers)
                    .withBody("{\"error\": \"" + message + "\"}");
        }
    }
}

