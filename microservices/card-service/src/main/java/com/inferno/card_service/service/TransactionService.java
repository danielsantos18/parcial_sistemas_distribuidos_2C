package com.inferno.card_service.service;

import com.inferno.card_service.dto.CardPurchaseRequest;
import com.inferno.card_service.dto.TransactionResponse;
import com.inferno.card_service.model.Card;
import com.inferno.card_service.model.Transaction;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.*;

public class TransactionService {

    private final CardService cardService;
    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Transaction> transactionTable;
    private final DynamoDbTable<Card> cardTable;
    private final DynamoDbClient dynamoDbClient;
    private final HttpClient httpClient;
    private static final String USER_SERVICE_URL = System.getenv("USER_SERVICE_URL");

    public TransactionService() {
        this.cardService = new CardService();
        this.dynamoDbClient = DynamoDbClient.create();
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.transactionTable = enhancedClient.table("transaction-table", TableSchema.fromBean(Transaction.class));
        this.cardTable = enhancedClient.table("card-table", TableSchema.fromBean(Card.class));
        this.httpClient = HttpClient.newHttpClient();
    }

    public TransactionResponse purchase(CardPurchaseRequest req) {
        String cardUuid = req.getCardId();
        Long amount = req.getAmount();

        System.out.println("💳 [PURCHASE START] Processing transaction for card: " + cardUuid + " amount: " + amount);

        // 1️⃣ Obtener la tarjeta más reciente
        List<Card> cards = cardService.getCardsByUuid(cardUuid);
        if (cards == null || cards.isEmpty()) {
            throw new RuntimeException("Card not found: " + cardUuid);
        }

        Card currentCard = cards.get(0);
        System.out.println("✅ Card found: " + currentCard.getType() + " | Balance: " + currentCard.getBalance() + " | Used: " + currentCard.getUsedBalance());

        // 2️⃣ Construir la key (PK + SK)
        Map<String, AttributeValue> key = Map.of(
                "uuid", AttributeValue.builder().s(currentCard.getUuid()).build(),
                "createdAt", AttributeValue.builder().s(currentCard.getCreatedAt()).build()
        );

        try {
            UpdateItemRequest updateReq;

            // =====================================================
            // 💳 DEBIT CARD
            // =====================================================
            if ("DEBIT".equalsIgnoreCase(currentCard.getType())) {

                Map<String, AttributeValue> exprVals = new HashMap<>();
                exprVals.put(":amt", AttributeValue.builder().n(String.valueOf(amount)).build());
                exprVals.put(":zero", AttributeValue.builder().n("0").build());
                exprVals.put(":one", AttributeValue.builder().n("1").build());
                exprVals.put(":now", AttributeValue.builder().s(Instant.now().toString()).build());

                String condition = "attribute_exists(balance) AND balance >= :amt";
                String updateExpr = "SET balance = balance - :amt, " +
                        "transactionsCount = if_not_exists(transactionsCount, :zero) + :one, " +
                        "updatedAt = :now";

                updateReq = UpdateItemRequest.builder()
                        .tableName(this.cardTable.tableName())
                        .key(key)
                        .updateExpression(updateExpr)
                        .conditionExpression(condition)
                        .expressionAttributeValues(exprVals)
                        .returnValues(ReturnValue.ALL_NEW)
                        .build();

                System.out.println("🔄 Updating DEBIT card balance in DynamoDB...");

            }
            // =====================================================
            // 💳 CREDIT CARD
            // =====================================================
            else if ("CREDIT".equalsIgnoreCase(currentCard.getType())) {

                Map<String, AttributeValue> exprVals = new HashMap<>();
                exprVals.put(":amt", AttributeValue.builder().n(String.valueOf(amount)).build());
                exprVals.put(":zero", AttributeValue.builder().n("0").build());
                exprVals.put(":one", AttributeValue.builder().n("1").build());
                exprVals.put(":now", AttributeValue.builder().s(Instant.now().toString()).build());

                long limit = currentCard.getLimit() == null ? 0L : currentCard.getLimit();
                long usedBalance = currentCard.getUsedBalance() == null ? 0L : currentCard.getUsedBalance();

                System.out.println("💳 Credit card limit: " + limit + " | Used: " + usedBalance);

                if (usedBalance + amount > limit) {
                    throw new RuntimeException("Credit limit exceeded");
                }

                String updateExpr = "SET usedBalance = if_not_exists(usedBalance, :zero) + :amt, " +
                        "transactionsCount = if_not_exists(transactionsCount, :zero) + :one, " +
                        "updatedAt = :now";

                updateReq = UpdateItemRequest.builder()
                        .tableName(this.cardTable.tableName())
                        .key(key)
                        .updateExpression(updateExpr)
                        .expressionAttributeValues(exprVals)
                        .returnValues(ReturnValue.ALL_NEW)
                        .build();

                System.out.println("🔄 Updating CREDIT card usedBalance in DynamoDB...");
            } else {
                throw new RuntimeException("Unsupported card type: " + currentCard.getType());
            }

            // 5️⃣ Ejecutar update atómico
            UpdateItemResponse updateResp = dynamoDbClient.updateItem(updateReq);
            Map<String, AttributeValue> updatedAttrs = updateResp.attributes();
            System.out.println("✅ Card updated successfully in DynamoDB: " + updatedAttrs);

            // 6️⃣ Guardar transacción
            Transaction transaction = new Transaction();
            transaction.setUuid(UUID.randomUUID().toString());
            transaction.setCardId(currentCard.getUuid());
            transaction.setAmount(amount);
            transaction.setMerchant(req.getMerchant());
            transaction.setType("PURCHASE");
            transaction.setCreatedAt(Instant.now().toString());
            transactionTable.putItem(transaction);

            System.out.println("🧾 Transaction saved successfully: " + transaction.getUuid());

            // 7️⃣ Activar tarjeta si corresponde
            if (updatedAttrs != null && updatedAttrs.containsKey("transactionsCount")) {
                int txCount = Integer.parseInt(updatedAttrs.get("transactionsCount").n());
                String currentStatus = updatedAttrs.containsKey("status")
                        ? updatedAttrs.get("status").s()
                        : currentCard.getStatus();

                if (txCount >= 10 && "PENDING".equalsIgnoreCase(currentStatus)) {
                    try {
                        Map<String, AttributeValue> actVals = new HashMap<>();
                        actVals.put(":newStatus", AttributeValue.builder().s("ACTIVATED").build());
                        actVals.put(":now", AttributeValue.builder().s(Instant.now().toString()).build());

                        String actUpdate = "SET #s = :newStatus, updatedAt = :now";

                        UpdateItemRequest activateReq = UpdateItemRequest.builder()
                                .tableName(System.getenv("CARD_TABLE"))
                                .key(key)
                                .updateExpression(actUpdate)
                                .expressionAttributeNames(Map.of("#s", "status"))
                                .expressionAttributeValues(actVals)
                                .build();

                        dynamoDbClient.updateItem(activateReq);
                        System.out.println("🎯 Card auto-activated after reaching 10 transactions.");
                    } catch (Exception e2) {
                        System.err.println("⚠️ Failed to auto-activate card: " + e2.getMessage());
                    }
                }
            }

            // 8️⃣ Retornar respuesta
            TransactionResponse response = new TransactionResponse();
            response.setUuid(transaction.getUuid());
            response.setCardId(currentCard.getUuid());
            response.setAmount(transaction.getAmount());
            response.setType(transaction.getType());
            response.setMerchant(transaction.getMerchant());
            response.setCreatedAt(transaction.getCreatedAt());

            System.out.println("✅ [PURCHASE SUCCESS] Transaction completed successfully.");
            return response;

        } catch (ConditionalCheckFailedException cex) {
            String msg = "Unknown condition failure";
            if ("DEBIT".equalsIgnoreCase(currentCard.getType())) msg = "Insufficient funds";
            else if ("CREDIT".equalsIgnoreCase(currentCard.getType())) msg = "Credit limit exceeded";
            System.err.println("⚠️ Condition failed: " + msg);
            throw new RuntimeException(msg, cex);
        } catch (Exception ex) {
            System.err.println("❌ Failed to process purchase: " + ex.getMessage());
            throw new RuntimeException("Failed to process purchase: " + ex.getMessage(), ex);
        }
    }

    public TransactionResponse deposit(CardPurchaseRequest req) {
        String cardUuid = req.getCardId();
        Long amount = req.getAmount();

        List<Card> cards = cardService.getCardsByUuid(cardUuid);
        if (cards.isEmpty()) throw new RuntimeException("Card not found");

        Card card = cards.get(0);
        if (!"DEBIT".equalsIgnoreCase(card.getType()))
            throw new RuntimeException("Only debit cards can receive deposits");

        Map<String, AttributeValue> key = Map.of(
                "uuid", AttributeValue.builder().s(card.getUuid()).build(),
                "createdAt", AttributeValue.builder().s(card.getCreatedAt()).build()
        );

        Map<String, AttributeValue> values = Map.ofEntries(
                Map.entry(":amt", AttributeValue.builder().n(amount.toString()).build()),
                Map.entry(":zero", AttributeValue.builder().n("0").build()),
                Map.entry(":one", AttributeValue.builder().n("1").build()),
                Map.entry(":now", AttributeValue.builder().s(Instant.now().toString()).build())
        );

        String update = "SET balance = if_not_exists(balance, :zero) + :amt, " +
                "transactionsCount = if_not_exists(transactionsCount, :zero) + :one, " +
                "updatedAt = :now";

        UpdateItemRequest reqUpdate = UpdateItemRequest.builder()
                .tableName(this.cardTable.tableName())
                .key(key)
                .updateExpression(update)
                .expressionAttributeValues(values)
                .returnValues(ReturnValue.ALL_NEW)
                .build();

        dynamoDbClient.updateItem(reqUpdate);

        // Guardar transacción
        Transaction tx = new Transaction();
        tx.setUuid(UUID.randomUUID().toString());
        tx.setCardId(cardUuid);
        tx.setAmount(amount);
        tx.setMerchant(req.getMerchant());
        tx.setType("DEPOSIT");
        tx.setCreatedAt(Instant.now().toString());
        transactionTable.putItem(tx);

        TransactionResponse resp = new TransactionResponse();
        resp.setUuid(tx.getUuid());
        resp.setCardId(tx.getCardId());
        resp.setAmount(tx.getAmount());
        resp.setType(tx.getType());
        resp.setMerchant(tx.getMerchant());
        resp.setCreatedAt(tx.getCreatedAt());

        return resp;
    }

    public TransactionResponse payCredit(CardPurchaseRequest req) {
        String cardUuid = req.getCardId();
        Long amount = req.getAmount();

        List<Card> cards = cardService.getCardsByUuid(cardUuid);
        if (cards.isEmpty()) throw new RuntimeException("Card not found");

        Card card = cards.get(0);
        if (!"CREDIT".equalsIgnoreCase(card.getType()))
            throw new RuntimeException("Only credit cards can be paid");

        long used = card.getUsedBalance() == null ? 0L : card.getUsedBalance();
        if (used <= 0) throw new RuntimeException("No debt to pay");

        Map<String, AttributeValue> key = Map.of(
                "uuid", AttributeValue.builder().s(card.getUuid()).build(),
                "createdAt", AttributeValue.builder().s(card.getCreatedAt()).build()
        );

        Map<String, AttributeValue> values = Map.ofEntries(
                Map.entry(":amt", AttributeValue.builder().n(amount.toString()).build()),
                Map.entry(":zero", AttributeValue.builder().n("0").build()),
                Map.entry(":one", AttributeValue.builder().n("1").build()),
                Map.entry(":now", AttributeValue.builder().s(Instant.now().toString()).build())
        );

        String condition = "usedBalance >= :amt";
        String update = "SET usedBalance = usedBalance - :amt, " +
                "transactionsCount = if_not_exists(transactionsCount, :zero) + :one, " +
                "updatedAt = :now";

        UpdateItemRequest reqUpdate = UpdateItemRequest.builder()
                .tableName(this.cardTable.tableName())
                .key(key)
                .updateExpression(update)
                .conditionExpression(condition)
                .expressionAttributeValues(values)
                .returnValues(ReturnValue.ALL_NEW)
                .build();

        dynamoDbClient.updateItem(reqUpdate);

        // Guardar transacción
        Transaction tx = new Transaction();
        tx.setUuid(UUID.randomUUID().toString());
        tx.setCardId(cardUuid);
        tx.setAmount(amount);
        tx.setMerchant(req.getMerchant());
        tx.setType("PAYMENT");
        tx.setCreatedAt(Instant.now().toString());
        transactionTable.putItem(tx);

        TransactionResponse resp = new TransactionResponse();
        resp.setUuid(tx.getUuid());
        resp.setCardId(tx.getCardId());
        resp.setAmount(tx.getAmount());
        resp.setType(tx.getType());
        resp.setMerchant(tx.getMerchant());
        resp.setCreatedAt(tx.getCreatedAt());

        return resp;
    }


}
