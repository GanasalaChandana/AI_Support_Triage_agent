package com.triage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI triageAgentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AI Support Triage Agent")
                .description("Submits customer support tickets to a Spring AI agent (RAG + tool-calling) "
                        + "that decides whether to reply, escalate, or create a tracking ticket.")
                .version("v1"));
    }
}
