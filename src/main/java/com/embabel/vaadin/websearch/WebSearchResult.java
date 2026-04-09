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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single web search result returned by the tool gateway.
 * Field names match the Brave Search MCP response structure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebSearchResult(
        String title,
        String url,
        String description,
        String age
) {
    /** Returns true if the result has a non-blank description. */
    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    /** Returns true if the result has an age/date string. */
    public boolean hasAge() {
        return age != null && !age.isBlank();
    }
}
