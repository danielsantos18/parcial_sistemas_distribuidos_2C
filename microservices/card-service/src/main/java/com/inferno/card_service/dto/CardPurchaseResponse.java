package com.inferno.card_service.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CardPurchaseResponse {
    private String transactionId;
    private Map<String, Object> card;

    public CardPurchaseResponse(String transactionId, Map<String, Object> card) {
        this.transactionId = transactionId;
        this.card = card;
    }
}
