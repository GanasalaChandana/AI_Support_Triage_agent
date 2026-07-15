package com.triage.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Calls Cohere's embed API directly instead of pulling in a local ONNX/DJL
 * model - the native PyTorch runtime needs more memory than fits on
 * free-tier cloud hosts (512MB). This has no heavy runtime, just an HTTP call.
 */
public class CohereEmbeddingModel implements EmbeddingModel {

    private final RestClient restClient;
    private final String model;

    public CohereEmbeddingModel(String apiKey, String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.cohere.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        CohereEmbedResponse response = restClient.post()
                .uri("/v1/embed")
                .body(new CohereEmbedRequest(request.getInstructions(), model, "search_document"))
                .retrieve()
                .body(CohereEmbedResponse.class);

        List<Embedding> embeddings = IntStream.range(0, response.embeddings().size())
                .mapToObj(i -> new Embedding(toFloatArray(response.embeddings().get(i)), i))
                .toList();

        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getFormattedContent());
    }

    private static float[] toFloatArray(List<Double> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }

    private record CohereEmbedRequest(List<String> texts, String model, String input_type) {
    }

    private record CohereEmbedResponse(List<List<Double>> embeddings) {
    }
}
