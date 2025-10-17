package com.inferno.catalog_service.dto;

import com.inferno.catalog_service.util.ServiceInfo;
import lombok.Data;

@Data
public class StartPaymentRequest {
    private String cardId;
    private ServiceInfo service;
}
