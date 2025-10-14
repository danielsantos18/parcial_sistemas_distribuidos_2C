package com.inferno.card_service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TransactionResponse {
    private String uuid;
    private String cardId;
    private Long amount;
    private String merchant;
    private String type;
    private String createdAt;
    private Map<String, Object> metadata;
}
