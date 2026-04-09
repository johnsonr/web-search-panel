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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Vaadin panel that provides a web search interface backed by the Embabel tool gateway.
 *
 * <p>Usage:
 * <pre>{@code
 * var client = new ToolGatewayClient("http://localhost:8080", objectMapper);
 * var panel = new WebSearchPanel(client::search);
 * add(panel);
 * }</pre>
 *
 * <p>The {@code searcher} function receives the query string and result count,
 * and returns a list of {@link WebSearchResult} objects. Inject any implementation —
 * a {@link ToolGatewayClient}, a mock, or a custom service.
 */
public class WebSearchPanel extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchPanel.class);
    private static final int DEFAULT_RESULT_COUNT = 10;

    private final BiFunction<String, Integer, List<WebSearchResult>> searcher;
    private final TextField queryField;
    private final Button searchButton;
    private final ProgressBar progressBar;
    private final VerticalLayout resultsContainer;
    private final Span resultsSummary;

    /**
     * Creates a WebSearchPanel with the default result count ({@value DEFAULT_RESULT_COUNT}).
     *
     * @param searcher function that accepts (query, count) and returns results
     */
    public WebSearchPanel(BiFunction<String, Integer, List<WebSearchResult>> searcher) {
        this(searcher, DEFAULT_RESULT_COUNT);
    }

    /**
     * Creates a WebSearchPanel with a custom result count.
     *
     * @param searcher    function that accepts (query, count) and returns results
     * @param resultCount number of results to request per search
     */
    public WebSearchPanel(BiFunction<String, Integer, List<WebSearchResult>> searcher, int resultCount) {
        this.searcher = searcher;
        setPadding(true);
        setSpacing(true);
        addClassName("web-search-panel");

        // --- Title ---
        var title = new H4("Web Search");
        title.addClassName("section-title");

        // --- Search bar ---
        queryField = new TextField();
        queryField.setPlaceholder("Enter search query...");
        queryField.setWidthFull();
        queryField.setClearButtonVisible(true);
        queryField.setPrefixComponent(VaadinIcon.SEARCH.create());
        queryField.addKeyPressListener(Key.ENTER, e -> performSearch(resultCount));

        searchButton = new Button("Search", VaadinIcon.SEARCH.create());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.addClickListener(e -> performSearch(resultCount));

        var searchBar = new HorizontalLayout(queryField, searchButton);
        searchBar.setWidthFull();
        searchBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        searchBar.setFlexGrow(1, queryField);

        // --- Progress bar (hidden by default) ---
        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.addClassName("search-progress");

        // --- Results summary ---
        resultsSummary = new Span();
        resultsSummary.addClassName("search-results-summary");
        resultsSummary.setVisible(false);

        // --- Results container ---
        resultsContainer = new VerticalLayout();
        resultsContainer.setPadding(false);
        resultsContainer.setSpacing(true);
        resultsContainer.addClassName("search-results-container");

        add(title, searchBar, progressBar, resultsSummary, resultsContainer);
    }

    private void performSearch(int resultCount) {
        var query = queryField.getValue();
        if (query == null || query.trim().isEmpty()) {
            Notification.show("Please enter a search query", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }

        var trimmedQuery = query.trim();
        setSearching(true);
        resultsContainer.removeAll();
        resultsSummary.setVisible(false);

        var ui = getUI().orElse(null);

        new Thread(() -> {
            try {
                logger.debug("Searching for: {}", trimmedQuery);
                var results = searcher.apply(trimmedQuery, resultCount);

                if (ui != null) {
                    ui.access(() -> {
                        setSearching(false);
                        showResults(trimmedQuery, results);
                    });
                }
            } catch (Exception e) {
                logger.error("Search failed for query '{}': {}", trimmedQuery, e.getMessage(), e);
                if (ui != null) {
                    ui.access(() -> {
                        setSearching(false);
                        Notification.show("Search failed: " + e.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            }
        }).start();
    }

    private void setSearching(boolean searching) {
        searchButton.setEnabled(!searching);
        queryField.setEnabled(!searching);
        progressBar.setVisible(searching);
    }

    private void showResults(String query, List<WebSearchResult> results) {
        if (results.isEmpty()) {
            resultsSummary.setText("No results found for \"" + query + "\"");
            resultsSummary.setVisible(true);
            return;
        }

        resultsSummary.setText(results.size() + " result" + (results.size() != 1 ? "s" : "") + " for \"" + query + "\"");
        resultsSummary.setVisible(true);

        for (var result : results) {
            resultsContainer.add(createResultCard(result));
        }
    }

    private Div createResultCard(WebSearchResult result) {
        var card = new Div();
        card.addClassName("search-result-card");

        // Title as a link
        var titleLink = new Anchor(result.url(), result.title());
        titleLink.addClassName("search-result-title");
        titleLink.setTarget("_blank");
        titleLink.getElement().setAttribute("rel", "noopener noreferrer");
        card.add(titleLink);

        // URL display
        var urlSpan = new Span(result.url());
        urlSpan.addClassName("search-result-url");
        card.add(urlSpan);

        // Description
        if (result.hasDescription()) {
            var desc = new Paragraph(result.description());
            desc.addClassName("search-result-description");
            card.add(desc);
        }

        // Age / date
        if (result.hasAge()) {
            var age = new Span(result.age());
            age.addClassName("search-result-age");
            card.add(age);
        }

        return card;
    }
}
