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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for the web search panel.
 * Registers a {@link ToolGatewayClient} bean using properties from
 * {@link WebSearchProperties}.
 *
 * <p>Add to {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 */
@AutoConfiguration
@EnableConfigurationProperties(WebSearchProperties.class)
public class WebSearchPanelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ToolGatewayClient toolGatewayClient(WebSearchProperties properties, ObjectMapper objectMapper) {
        return new ToolGatewayClient(properties.getBaseUrl(), objectMapper);
    }
}
