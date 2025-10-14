package com.inferno.card_service.service;

import com.inferno.card_service.model.Card;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CardService {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Card> cardTable;
    private final DynamoDbClient dynamoDbClient;  // 👈 agregado
    private final HttpClient httpClient;
    private static final Logger logger = Logger.getLogger(CardService.class.getName());
    private static final String USER_SERVICE_URL = System.getenv("USER_SERVICE_URL");

    public CardService() {
        this.dynamoDbClient = DynamoDbClient.create();
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        this.cardTable = enhancedClient.table("card-table", TableSchema.fromBean(Card.class));
        this.httpClient = HttpClient.newHttpClient();
    }

    public Card createCard(String userId, String requestType) {
        // 1. Primero verificar que el usuario existe
        if (!userExists(userId)) {
            throw new IllegalArgumentException("User does not exist: " + userId);
        }

        // 2. Crear la tarjeta
        Card card = new Card();
        card.setUuid(UUID.randomUUID().toString());
        card.setUserId(userId);
        card.setType(requestType.toUpperCase());
        card.setCreatedAt(Instant.now().toString());

        if ("DEBIT".equalsIgnoreCase(requestType)) {
            createDebitCard(card);
        } else if ("CREDIT".equalsIgnoreCase(requestType)) {
            createCreditCard(card);
        } else {
            throw new IllegalArgumentException("Invalid card type: " + requestType);
        }

        cardTable.putItem(card);
        logger.info("Card created successfully for user: " + userId);
        return card;
    }

    private boolean userExists(String userId) {
        try {
            String url = USER_SERVICE_URL + "/users/profile/" + userId;
            logger.info("Checking user existence: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("User service response: " + response.statusCode());

            // Si responde 200 OK, el usuario existe
            return response.statusCode() == 200;

        } catch (Exception e) {
            logger.severe("Error checking user existence: " + e.getMessage());
            throw new RuntimeException("Unable to verify user existence: " + e.getMessage());
        }
    }

    /**
     * Obtiene una card específica por uuid y createdAt
     */
    public Card getCard(String uuid, String createdAt) {
        try {
            Key key = Key.builder()
                    .partitionValue(uuid)
                    .sortValue(createdAt)
                    .build();

            return cardTable.getItem(key);
        } catch (Exception e) {
            logger.severe("Error retrieving card with uuid: " + uuid + " and createdAt: " + createdAt + " - " + e.getMessage());
            throw new RuntimeException("Failed to retrieve card", e);
        }
    }

    /**
     * Obtiene todas las cards para un uuid específico (sin importar el createdAt)
     */
    public List<Card> getCardsByUuid(String uuid) {
        try {
            QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder()
                    .partitionValue(uuid)
                    .build());

            return cardTable.query(queryConditional)
                    .items()
                    .stream()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.severe("Error retrieving cards for uuid: " + uuid + " - " + e.getMessage());
            throw new RuntimeException("Failed to retrieve cards by uuid", e);
        }
    }

    private void createDebitCard(Card card) {
        card.setStatus("ACTIVATED");
        card.setBalance(0L);
        card.setScore(0);
    }

    private void createCreditCard(Card card) {
        int score = ThreadLocalRandom.current().nextInt(0, 101);
        long limit = computeCreditLimit(score);

        card.setStatus("PENDING");
        card.setLimit(limit);
        card.setUsedBalance(0L);
        card.setScore(score);
        card.setBalance(0L);
    }

    private long computeCreditLimit(int score) {
        double amount = 100 + (score / 100.0) * (10000000 - 100);
        return Math.round(amount);
    }

    /**
     * Busca todas las tarjetas PENDING de un usuario - CORREGIDO
     */
    public List<Card> findPendingCardsByUser(String userId) {
        try {
            // Método más simple: scan y filtrar manualmente
            var items = cardTable.scan().items().iterator();
            List<Card> pendingCards = new ArrayList<>();

            while (items.hasNext()) {
                Card card = items.next();
                if (userId.equals(card.getUserId()) && "PENDING".equals(card.getStatus())) {
                    pendingCards.add(card);
                }
            }

            logger.info("Found " + pendingCards.size() + " pending cards for user: " + userId);
            return pendingCards;

        } catch (Exception e) {
            logger.severe("Error finding pending cards: " + e.getMessage());
            throw new RuntimeException("Error searching pending cards", e);
        }
    }

    /**
     * Activa una tarjeta (cambia status de PENDING a ACTIVATED)
     */
    public void activateCard(Card card) {
        try {
            card.setStatus("ACTIVATED");
            cardTable.updateItem(card);
            logger.info("Card activated: " + card.getUuid());

        } catch (Exception e) {
            logger.severe("Error activating card: " + card.getUuid() + " - " + e.getMessage());
            throw new RuntimeException("Error activating card", e);
        }
    }

    /**
     * Activa todas las tarjetas pendientes de un usuario
     */
    public int activateAllPendingCards(String userId) {
        List<Card> pendingCards = findPendingCardsByUser(userId);

        if (pendingCards.isEmpty()) {
            return 0;
        }

        int activatedCount = 0;
        for (Card card : pendingCards) {
            activateCard(card);
            activatedCount++;
        }

        logger.info("Activated " + activatedCount + " cards for user: " + userId);
        return activatedCount;
    }

    // =============================================================
    // 💰 ACTUALIZACIONES DE BALANCE
    // =============================================================
    public Card updateDebitCardBalance(String cardId, Long amount) {
        List<Card> card = getCardsByUuid(cardId);
        if (card == null) throw new RuntimeException("Card not found");

        try {
            Map<String, AttributeValue> key = Map.of(
                    "uuid", AttributeValue.builder().s(card.get(0).getUuid()).build(),
                    "createdAt", AttributeValue.builder().s(card.get(0).getCreatedAt()).build()
            );

            Map<String, AttributeValue> values = Map.ofEntries(
                    Map.entry(":amount", AttributeValue.builder().n(amount.toString()).build()),
                    Map.entry(":zero", AttributeValue.builder().n("0").build()),
                    Map.entry(":one", AttributeValue.builder().n("1").build()),
                    Map.entry(":now", AttributeValue.builder().s(Instant.now().toString()).build())
            );

            String condition = "balance >= :amount";
            String update = "SET balance = balance - :amount, " +
                    "transactionsCount = if_not_exists(transactionsCount, :zero) + :one, " +
                    "updatedAt = :now";

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName("card-table")
                    .key(key)
                    .updateExpression(update)
                    .conditionExpression(condition)
                    .expressionAttributeValues(values)
                    .returnValues(ReturnValue.ALL_NEW)
                    .build();

            Map<String, AttributeValue> updated = dynamoDbClient.updateItem(request).attributes();
            return convertToCard(updated);

        } catch (ConditionalCheckFailedException e) {
            throw new RuntimeException("Insufficient funds");
        } catch (Exception e) {
            logger.severe("Error updating debit card: " + e.getMessage());
            throw new RuntimeException("Failed to update debit card", e);
        }
    }

    public Card updateCreditCardBalance(String cardId, Long amount) {
        List<Card> card = getCardsByUuid(cardId);
        if (card == null) throw new RuntimeException("Card not found");

        try {

            Map<String, AttributeValue> key = Map.of(
                    "uuid", AttributeValue.builder().s(card.get(0).getUuid()).build(),
                    "createdAt", AttributeValue.builder().s(card.get(0).getCreatedAt()).build()
            );

            Map<String, AttributeValue> values = Map.ofEntries(
                    Map.entry(":amount", AttributeValue.builder().n(amount.toString()).build()),
                    Map.entry(":zero", AttributeValue.builder().n("0").build()),
                    Map.entry(":one", AttributeValue.builder().n("1").build()),
                    Map.entry(":now", AttributeValue.builder().s(Instant.now().toString()).build())
                    //Map.entry(":limit", AttributeValue.builder().n(card.get(0).getLimit().toString()).build())
            );

            // 💳 Tarjeta Crédito → verificar límite antes de actualizar
            long limit = card.get(0).getLimit() == null ? 0L : card.get(0).getLimit();
            long usedBalance = card.get(0).getUsedBalance() == null ? 0L : card.get(0).getUsedBalance();

            if (usedBalance + amount.longValue() > limit) {
                throw new RuntimeException("Credit limit exceeded");
            }

            String update = "SET usedBalance = if_not_exists(usedBalance, :zero) + :amount, " +
                    "transactionsCount = if_not_exists(transactionsCount, :zero) + :one, " +
                    "updatedAt = :now";

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName("card-table")
                    .key(key)
                    .updateExpression(update)
                    .expressionAttributeValues(values)
                    .returnValues(ReturnValue.ALL_NEW)
                    .build();

            Map<String, AttributeValue> updated = dynamoDbClient.updateItem(request).attributes();
            return convertToCard(updated);

        } catch (ConditionalCheckFailedException e) {
            throw new RuntimeException("Credit limit exceeded");
        } catch (Exception e) {
            logger.severe("Error updating credit card: " + e.getMessage());
            throw new RuntimeException("Failed to update credit card", e);
        }
    }

    // =============================================================
    // 🧩 UTILIDAD
    // =============================================================
    private Card convertToCard(Map<String, AttributeValue> attrs) {
        Card card = new Card();
        card.setUuid(attrs.get("uuid").s());
        card.setCreatedAt(attrs.get("createdAt").s());
        if (attrs.containsKey("type")) card.setType(attrs.get("type").s());
        if (attrs.containsKey("balance")) card.setBalance(Long.parseLong(attrs.get("balance").n()));
        if (attrs.containsKey("limit")) card.setLimit(Long.parseLong(attrs.get("limit").n()));
        if (attrs.containsKey("usedBalance")) card.setUsedBalance(Long.parseLong(attrs.get("usedBalance").n()));
        if (attrs.containsKey("transactionsCount"))
            card.setTransactionsCount(Integer.parseInt(attrs.get("transactionsCount").n()));
        if (attrs.containsKey("updatedAt")) card.setUpdatedAt(attrs.get("updatedAt").s());
        return card;
    }
}