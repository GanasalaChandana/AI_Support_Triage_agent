package com.triage.rag;

import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    @Bean
    public CohereEmbeddingModel embeddingModel(
            @Value("${cohere.api-key}") String apiKey,
            @Value("${cohere.embedding-model:embed-english-v3.0}") String model) {
        return new CohereEmbeddingModel(apiKey, model);
    }

    @Bean
    public VectorStore vectorStore(CohereEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
