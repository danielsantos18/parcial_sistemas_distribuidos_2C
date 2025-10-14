package com.inferno.card_service.model;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.Map;

@Data
@DynamoDbBean
public class Transaction {
    private String uuid;
    private String cardId;
    private Long amount;
    private String merchant;
    private String type;
    private String createdAt;
    private Map<String, String> metadata;

    // Partition key
    @DynamoDbPartitionKey
    public String getUuid() {
        return uuid;
    }

    @DynamoDbSortKey
    @DynamoDbSecondaryPartitionKey(indexNames = "createdAt")
    public String getCreatedAt() {
        return createdAt;
    }
}
