package com.inferno.card_service.dto;

import lombok.Data;

@Data
public class CardPurchaseRequest {
    private String cardId;
    private String merchant;
    private Long amount;
}