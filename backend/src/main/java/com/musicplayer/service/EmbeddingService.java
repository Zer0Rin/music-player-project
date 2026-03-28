package com.musicplayer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    @Value("${siliconflow.api.key}")
    private String apiKey;

    @Value("${siliconflow.api.url}")
    private String apiUrl;

    @Value("${siliconflow.embedding.model}")
    private String model;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将文本转为 embedding 向量，返回 float[]
     * 失败时返回 null（不阻断主流程）
     */
    public float[] embed(String text) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "input", List.of(text),
                    "encoding_format", "float"
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode embeddingNode = root.path("data").get(0).path("embedding");

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            return vector;

        } catch (Exception e) {
            System.err.println("[EmbeddingService] 生成 embedding 失败: " + e.getMessage());
            return null;
        }
    }

    /** float[] 转 JSON 字符串存数据库 */
    public String toJson(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON 字符串转 float[] */
    public float[] fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            float[] result = objectMapper.readValue(json, float[].class);
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}