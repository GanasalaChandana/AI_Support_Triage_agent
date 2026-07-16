package com.triage.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Creates real issues in a Jira Cloud project via the REST API v3. Priority
 * is folded into the summary text rather than set as a Jira field, since
 * priority schemes vary per project/site and an unmapped value would fail
 * issue creation outright.
 */
@Component
public class JiraClient {

    private final RestClient restClient;
    private final String projectKey;

    public JiraClient(
            @Value("${jira.base-url}") String baseUrl,
            @Value("${jira.email}") String email,
            @Value("${jira.api-token}") String apiToken,
            @Value("${jira.project-key}") String projectKey) {
        this.projectKey = projectKey;
        String credentials = Base64.getEncoder().encodeToString((email + ":" + apiToken).getBytes());
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Basic " + credentials)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String createIssue(String summary, String priority) {
        Map<String, Object> body = Map.of(
                "fields", Map.of(
                        "project", Map.of("key", projectKey),
                        "summary", "[" + priority + "] " + summary,
                        "description", adfDescription(summary),
                        "issuetype", Map.of("name", "Task")
                )
        );

        JiraIssueResponse response = restClient.post()
                .uri("/rest/api/3/issue")
                .body(body)
                .retrieve()
                .body(JiraIssueResponse.class);

        return response.key();
    }

    private Map<String, Object> adfDescription(String text) {
        return Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", text))
                ))
        );
    }

    private record JiraIssueResponse(String id, String key, String self) {
    }
}
