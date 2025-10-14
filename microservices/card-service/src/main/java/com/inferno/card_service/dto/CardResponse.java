package com.inferno.card_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardResponse {
    private String uuid;
    private String userId;
    private String type;        // CREDIT or DEBIT
    private String status;      // PENDING or ACTIVATED
    private Long balance;       // solo DEBIT
    private Long limit;         // solo CREDIT
    private Long usedBalance;   // solo CREDIT
    private Integer score;      // solo CREDIT
    private String createdAt;
}

