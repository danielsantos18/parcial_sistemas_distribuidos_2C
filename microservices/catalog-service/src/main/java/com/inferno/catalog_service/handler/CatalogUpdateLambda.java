package com.inferno.catalog_service.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public class CatalogUpdateLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final S3Client s3 = S3Client.create();
    private final ObjectMapper mapper = new ObjectMapper();
    private final RedisClient redisClient = RedisClient.create(System.getenv("REDIS_URI"));
    private final String bucket = System.getenv("BUCKET_NAME");
    private final String redisKey = System.getenv("CATALOG_REDIS_KEY");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context ctx) {
        try {
            ctx.getLogger().log("Request received\n");

            //Decodificar CSV desde Base64
            if (req.getBody() == null || req.getBody().isEmpty()) {
                throw new RuntimeException("Request body is empty");
            }

            byte[] csvBytes = req.getIsBase64Encoded() != null && req.getIsBase64Encoded()
                    ? Base64.getDecoder().decode(req.getBody())
                    : req.getBody().getBytes(StandardCharsets.UTF_8);

            ctx.getLogger().log("CSV decoded, size: " + csvBytes.length + " bytes\n");

            //Subir CSV a S3
            String filename = "catalog-" + Instant.now() + ".csv";
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(filename)
                    .contentType("text/csv")
                    .build();

            s3.putObject(put, RequestBody.fromBytes(csvBytes));
            ctx.getLogger().log("Uploaded CSV to S3: " + filename + "\n");

            //Parsear CSV
            List<Map<String, Object>> catalog = parseCsvToList(csvBytes);
            ctx.getLogger().log("Parsed CSV rows: " + catalog.size() + "\n");

            //Guardar JSON completo en Redis
            try (StatefulRedisConnection<String, String> conn = redisClient.connect()) {
                RedisCommands<String, String> cmd = conn.sync();
                String json = mapper.writeValueAsString(catalog);
                cmd.set(redisKey, json);
                ctx.getLogger().log("Catalog data saved in Redis under key: " + redisKey + "\n");
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{\"status\":\"ok\",\"s3Key\":\"" + filename + "\"}");

        } catch (Exception e) {
            ctx.getLogger().log("Error processing CSV: " + e.getMessage() + "\n");
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private List<Map<String, Object>> parseCsvToList(byte[] csvBytes) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            for (CSVRecord rec : parser) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String header : parser.getHeaderMap().keySet()) {
                    row.put(header, rec.get(header));
                }
                list.add(row);
            }
        }
        return list;
    }
}
