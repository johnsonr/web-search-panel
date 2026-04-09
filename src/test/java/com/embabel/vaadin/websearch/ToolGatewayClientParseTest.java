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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link ToolGatewayClient} correctly parses all response shapes
 * that the tool gateway may return.
 */
class ToolGatewayClientParseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Access private parseResults via reflection for unit testing. */
    @SuppressWarnings("unchecked")
    private List<WebSearchResult> parseResults(String json) throws Exception {
        // Create a client with a dummy base URL — we only test parseResults
        var client = new ToolGatewayClient("http://localhost:9999", objectMapper);
        Method m = ToolGatewayClient.class.getDeclaredMethod("parseResults", String.class);
        m.setAccessible(true);
        return (List<WebSearchResult>) m.invoke(client, json);
    }

    @Test
    void parsesDirectArray() throws Exception {
        var json = """
                [
                  {"title": "Example", "url": "https://example.com", "description": "An example site", "age": "2025-01-01"},
                  {"title": "Other",   "url": "https://other.com",   "description": "Another site"}
                ]
                """;

        var results = parseResults(json);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).title()).isEqualTo("Example");
        assertThat(results.get(0).url()).isEqualTo("https://example.com");
        assertThat(results.get(0).age()).isEqualTo("2025-01-01");
        assertThat(results.get(1).title()).isEqualTo("Other");
        assertThat(results.get(1).age()).isNull();
    }

    @Test
    void parsesResultsWrapped() throws Exception {
        var json = """
                {
                  "results": [
                    {"title": "Wrapped", "url": "https://wrapped.com", "description": "Wrapped result"}
                  ]
                }
                """;

        var results = parseResults(json);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("Wrapped");
    }

    @Test
    void parsesDoubleWrapped() throws Exception {
        // Brave MCP raw shape: {"web": {"results": [...]}}
        var json = """
                {
                  "web": {
                    "results": [
                      {"title": "Deep", "url": "https://deep.com", "description": "Deep result"}
                    ]
                  }
                }
                """;

        var results = parseResults(json);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("Deep");
    }

    @Test
    void parsesSingleKeyWrappedArray() throws Exception {
        // Gateway unwraps {"web": [...]} to [...] — test the already-unwrapped form
        var json = """
                {
                  "web": [
                    {"title": "Single", "url": "https://single.com", "description": "Single-key wrapped array"}
                  ]
                }
                """;

        var results = parseResults(json);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("Single");
    }

    @Test
    void returnsEmptyListForEmptyArray() throws Exception {
        var results = parseResults("[]");
        assertThat(results).isEmpty();
    }

    @Test
    void webSearchResultHasDescriptionAndAge() {
        var withBoth = new WebSearchResult("T", "http://x.com", "desc", "2025-06-01");
        assertThat(withBoth.hasDescription()).isTrue();
        assertThat(withBoth.hasAge()).isTrue();

        var withNeither = new WebSearchResult("T", "http://x.com", null, null);
        assertThat(withNeither.hasDescription()).isFalse();
        assertThat(withNeither.hasAge()).isFalse();

        var withBlank = new WebSearchResult("T", "http://x.com", "  ", "");
        assertThat(withBlank.hasDescription()).isFalse();
        assertThat(withBlank.hasAge()).isFalse();
    }
}
