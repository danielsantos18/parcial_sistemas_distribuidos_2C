package com.inferno.card_service.dto;

import lombok.Data;

@Data
public class CardManageRequest {
    private String merchant;
    private Long amount;
}
