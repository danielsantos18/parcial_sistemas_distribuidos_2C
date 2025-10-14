package com.inferno.card_service.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inferno.card_service.dto.CardPurchaseRequest;
import com.inferno.card_service.dto.TransactionResponse;
import com.inferno.card_service.service.TransactionService;

import java.util.Map;

public class CardPaidCreditLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final TransactionService transactionService = new TransactionService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "Access-Control-Allow-Origin", "*"
        );

        try {
            String cardId = request.getPathParameters().get("card_id");
            CardPurchaseRequest body = objectMapper.readValue(request.getBody(), CardPurchaseRequest.class);
            body.setCardId(cardId);

            TransactionResponse resp = transactionService.payCredit(body);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(objectMapper.writeValueAsString(resp));

        } catch (Exception e) {
            e.printStackTrace();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
