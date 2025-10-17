package com.inferno.catalog_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;
import java.util.Map;

@Data
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private String traceId;
    private String userId;
    private String cardId;
    private Map<String, String> service;
    private String status;
    private String error;
    private String createdAt;
    private String UpdatedAt;
    private List<String> events;

    // Partition key
    @DynamoDbPartitionKey
    public String getTraceId() {
        return traceId;
    }

}