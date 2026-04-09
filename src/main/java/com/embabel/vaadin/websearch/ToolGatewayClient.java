/*
 * Copyright 2024-2026 Embabel Pty Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.vaadin.websearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for the Embabel tool gateway.
 * Calls {@code POST /api/v1/tools/{name}} and parses the JSON response
 * into a list of {@link WebSearchResult} objects.
 *
 * <p>The tool gateway normalises MCP responses: single-key JSON objects are
 * unwrapped, so {@code {"web": {"results": [...]}}} becomes {@code [...]}.
 * This client handles both a pre-unwrapped array and the raw MCP shapes.
 */
public class ToolGatewayClient {

    private static final Logger logger = LoggerFactory.getLogger(ToolGatewayClient.class);
    private static final TypeReference<List<WebSearchResult>> RESULT_LIST_TYPE =
            new TypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * @param baseUrl      Base URL of the assistant, e.g. {@code http://localhost:8080}
     * @param objectMapper Jackson mapper (can share with application context)
     */
    public ToolGatewayClient(String baseUrl, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Invokes {@code brave_web_search} via the tool gateway and returns
     * the normalised list of results.
     *
     * @param query the search query
     * @param count number of results to request (1-20)
     * @return list of search results, never null
     */
    public List<WebSearchResult> search(String query, int count) {
        var body = Map.of("query", query, "count", count);
        try {
            var raw = restClient.post()
                    .uri("/api/v1/tools/brave_web_search")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (raw == null || raw.isBlank()) {
                return List.of();
            }

            return parseResults(raw);
        } catch (Exception e) {
            logger.error("Tool gateway search failed for query '{}': {}", query, e.getMessage(), e);
            throw new WebSearchException("Search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the gateway response, handling all normalisation shapes:
     * <ul>
     *   <li>Array: {@code [{title, url, ...}, ...]}</li>
     *   <li>Wrapped: {@code {"results": [...]}}</li>
     *   <li>Double-wrapped: {@code {"web": {"results": [...]}}}</li>
     * </ul>
     */
    private List<WebSearchResult> parseResults(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);

        if (node.isArray()) {
            return objectMapper.convertValue(node, RESULT_LIST_TYPE);
        }

        // Unwrap single-key objects (mirrors gateway normalisation logic)
        while (node.isObject() && node.size() == 1) {
            node = node.fields().next().getValue();
        }

        if (node.isArray()) {
            return objectMapper.convertValue(node, RESULT_LIST_TYPE);
        }

        // Results may be inside a "results" field in a multi-key object
        if (node.isObject() && node.has("results")) {
            return objectMapper.convertValue(node.get("results"), RESULT_LIST_TYPE);
        }

        logger.warn("Unexpected tool gateway response shape: {}", json);
        return List.of();
    }
}
