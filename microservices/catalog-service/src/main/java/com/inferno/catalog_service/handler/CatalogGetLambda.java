package com.inferno.catalog_service.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.Map;

public class CatalogGetLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RedisClient redisClient = RedisClient.create(System.getenv("REDIS_URI"));
    private final String redisKey = System.getenv("CATALOG_REDIS_KEY");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context ctx) {
        ctx.getLogger().log("CatalogGetLambda invoked\n");

        try (StatefulRedisConnection<String, String> conn = redisClient.connect()) {
            RedisCommands<String, String> cmd = conn.sync();

            String json = cmd.get(redisKey);
            if (json == null) {
                ctx.getLogger().log("No catalog found in Redis for key: " + redisKey + "\n");
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody("{\"error\":\"Catalog not found\"}");
            }

            ctx.getLogger().log("Catalog retrieved from Redis\n");
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(json);

        } catch (Exception e) {
            ctx.getLogger().log("Error fetching catalog: " + e.getMessage() + "\n");
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
