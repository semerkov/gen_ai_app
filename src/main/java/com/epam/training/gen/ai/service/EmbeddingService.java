package com.epam.training.gen.ai.service;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

/**
 * Service class for processing text into embeddings and interacting with Qdrant for vector storage and retrieval.
 * <p>
 * This service converts text into embeddings using Azure OpenAI and saves these vectors in a Qdrant collection.
 * It also provides functionality to search for similar vectors based on input text.
 */
@Slf4j
@Service
public class EmbeddingService {

    private static final String ID_KEY = "id";
    private static final String PAYLOAD_KEY = "info";

    @Value("${embedding.deployment-name}")
    private String embeddingDeploymentName;

    @Value("${embedding.collection.size}")
    private long collectionSize;

    @Value("${embedding.results.limit}")
    private long limit;

    private final OpenAIAsyncClient aiAsyncClient;
    private final QdrantClient qdrantClient;

    @Autowired
    public EmbeddingService(OpenAIAsyncClient aiAsyncClient, QdrantClient qdrantClient) {

        this.aiAsyncClient = aiAsyncClient;
        this.qdrantClient = qdrantClient;
    }

    /**
     * Processes the input text into embeddings, transforms them into vector points,
     * and saves them in the Qdrant collection.
     *
     * @param id             identifier
     * @param text           the text to be processed into embeddings
     * @param collectionName collection name
     * @throws ExecutionException   if the vector saving operation fails
     * @throws InterruptedException if the thread is interrupted during execution
     */
    public void processAndSaveText(PointId id, String text, String collectionName)
            throws ExecutionException, InterruptedException {

        var embeddings = getEmbeddings(text);
        var points = new ArrayList<List<Float>>();
        embeddings.forEach(
                embeddingItem -> {
                    var values = new ArrayList<>(embeddingItem.getEmbedding());
                    points.add(values);
                });

        var pointStructs = new ArrayList<PointStruct>();
        points.forEach(point -> {
            var pointStruct = buildPointStruct(id, point, text);
            pointStructs.add(pointStruct);
        });

        saveVector(pointStructs, collectionName);
    }

    /**
     * Searches the Qdrant collection for vectors similar to the input text.
     * <p>
     * The input text is converted to embeddings, and a search is performed based on the vector similarity.
     *
     * @param text           the text to search for similar vectors
     * @param collectionName collection name
     * @param ids            identifiers to search among
     * @return a list of scored points representing similar vectors
     * @throws ExecutionException   if the search operation fails
     * @throws InterruptedException if the thread is interrupted during execution
     */
    public List<ScoredPoint> search(String text, String collectionName, List<String> ids)
            throws ExecutionException, InterruptedException {

        var embeddings = retrieveEmbeddings(text);
        var qe = new ArrayList<Float>();
        embeddings.block().getData().forEach(embeddingItem ->
                qe.addAll(embeddingItem.getEmbedding())
        );

        Points.SearchPoints.Builder builder = SearchPoints.newBuilder()
                .setCollectionName(collectionName)
                .addAllVector(qe)
                .setWithPayload(enable(true))
                .setLimit(limit);

        if (CollectionUtils.isNotEmpty(ids)) {
            List<Points.Condition> idConditions = ids.stream()
                    .map(id -> matchKeyword(ID_KEY, id))
                    .toList();

            builder.setFilter(
                    Filter.newBuilder()
                            .addAllShould(idConditions)
                            .build());
        }

        return qdrantClient
                .searchAsync(builder.build())
                .get();
    }

    /**
     * Retrieves the embeddings for the given text using Azure OpenAI.
     *
     * @param text the text to be embedded
     * @return a list of {@link EmbeddingItem} representing the embeddings
     */
    public List<EmbeddingItem> getEmbeddings(String text) {

        var embeddings = retrieveEmbeddings(text);
        return embeddings.block().getData();
    }

    /**
     * Creates a new collection in Qdrant with specified vector parameters.
     *
     * @param collectionName collection name
     * @throws ExecutionException   if the collection creation operation fails
     * @throws InterruptedException if the thread is interrupted during execution
     */
    public void createCollection(String collectionName) throws ExecutionException, InterruptedException {

        var result = qdrantClient.createCollectionAsync(collectionName,
                        VectorParams.newBuilder()
                                .setDistance(Collections.Distance.Cosine)
                                .setSize(collectionSize)
                                .build())
                .get();
        log.info("Collection was created: [{}]", result.getResult());
    }

    /**
     * Saves the list of point structures (vectors) to the Qdrant collection.
     *
     * @param pointStructs   the list of vectors to be saved
     * @param collectionName collection name
     * @throws InterruptedException if the thread is interrupted during execution
     * @throws ExecutionException   if the saving operation fails
     */
    private void saveVector(ArrayList<PointStruct> pointStructs, String collectionName)
            throws InterruptedException, ExecutionException {

        var updateResult = qdrantClient.upsertAsync(collectionName, pointStructs).get();
        log.info(updateResult.getStatus().name());
    }

    /**
     * Constructs a point structure from a list of float values representing a vector.
     * <p>
     * The Points.SearchPoints.Builder does not support filtering directly by point IDs as part of its standard
     * filtering mechanism because it primarily supports query vector similarity search, combined optionally
     * with payload-based filtering.
     *
     * @param id    identifier
     * @param point the vector values
     * @param text  text value
     * @return a {@link PointStruct} object containing the vector and associated metadata
     */
    private PointStruct buildPointStruct(PointId id, List<Float> point, String text) {

        String idValue = id.hasUuid() ? id.getUuid() : Long.toString(id.getNum());

        return PointStruct.newBuilder()
                .setId(id)
                .setVectors(vectors(point))
                .putAllPayload(Map.of(PAYLOAD_KEY, value(text), ID_KEY, value(idValue)))
                .build();
    }

    /**
     * Retrieves the embeddings for the given text asynchronously from Azure OpenAI.
     *
     * @param text the text to be embedded
     * @return a {@link Mono} of {@link Embeddings} representing the embeddings
     */
    private Mono<Embeddings> retrieveEmbeddings(String text) {

        var embeddingsOptions = new EmbeddingsOptions(List.of(text));
        return aiAsyncClient.getEmbeddings(embeddingDeploymentName, embeddingsOptions);
    }
}
