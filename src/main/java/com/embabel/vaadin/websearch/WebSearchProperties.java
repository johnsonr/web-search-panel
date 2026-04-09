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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the web search panel.
 *
 * <pre>{@code
 * embabel:
 *   web-search:
 *     base-url: http://localhost:8080
 *     result-count: 10
 * }</pre>
 */
@ConfigurationProperties(prefix = "embabel.web-search")
public class WebSearchProperties {

    /** Base URL of the Embabel assistant providing the tool gateway. */
    private String baseUrl = "http://localhost:8080";

    /** Number of results to request per search. */
    private int resultCount = 10;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }
}
