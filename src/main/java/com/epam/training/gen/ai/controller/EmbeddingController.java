package com.epam.training.gen.ai.controller;

import static io.qdrant.client.PointIdFactory.id;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.azure.ai.openai.models.EmbeddingItem;
import com.epam.training.gen.ai.model.request.EmbeddingRequestDto;
import com.epam.training.gen.ai.model.response.EmbeddingResponseDto;
import com.epam.training.gen.ai.service.EmbeddingService;

@RestController
@RequestMapping("/embedding")
public class EmbeddingController {

    private static final String PAYLOAD_KEY = "info";

    private final EmbeddingService embeddingService;

    @Value("${embedding.collection.name}")
    private String collectionName;

    @Autowired
    public EmbeddingController(EmbeddingService embeddingService) {

        this.embeddingService = embeddingService;
    }

    @PostMapping("/create-collection")
    public ResponseEntity<Void> createCollection() throws ExecutionException, InterruptedException {

        embeddingService.createCollection(collectionName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/build")
    public List<EmbeddingItem> getEmbeddings(@Validated @RequestBody EmbeddingRequestDto requestDto) {

        return embeddingService.getEmbeddings(requestDto.getText());
    }

    @PostMapping("/save")
    public ResponseEntity<Void> save(@Validated @RequestBody EmbeddingRequestDto requestDto)
            throws ExecutionException, InterruptedException {

        embeddingService.processAndSaveText(id(UUID.randomUUID()), requestDto.getText(), collectionName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/search")
    public List<EmbeddingResponseDto> search(@Validated @RequestBody EmbeddingRequestDto requestDto)
            throws ExecutionException, InterruptedException {

        return embeddingService.search(requestDto.getText(), collectionName, null).stream()
                .map(result -> new EmbeddingResponseDto(result.getId().getUuid(),
                        result.getPayloadMap().get(PAYLOAD_KEY).getStringValue(),
                        result.getScore()))
                .toList();
    }
}
