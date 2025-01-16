package com.epam.training.gen.ai.dating.controller;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epam.training.gen.ai.dating.converter.CandidateConverter;
import com.epam.training.gen.ai.dating.domain.Candidate;
import com.epam.training.gen.ai.dating.dto.PreferenceDto;
import com.epam.training.gen.ai.dating.dto.request.CandidateRequestDto;
import com.epam.training.gen.ai.dating.service.CandidateService;
import com.epam.training.gen.ai.model.response.PromptResponseDto;
import com.epam.training.gen.ai.service.EmbeddingService;
import com.epam.training.gen.ai.service.SemanticKernelService;

@RestController
@RequestMapping("/dating")
public class DatingController {

    private static final String PROMPT_TEMPLATE =
            "Provide a short description for the destiny number of the current candidate and the results found for him. "
                    + "Perform this action for the current candidate with destiny number %d. "
                    + "Provide information about the found candidates in the form of a table (each row from new line). "
                    + "For each row in the table, provide a description of the relationship of the destiny numbers with the current candidate. "
                    + "Current candidate username: %s.";

    private final SemanticKernelService semanticKernelService;
    private final CandidateService candidateService;
    private final CandidateConverter candidateConverter;
    private final EmbeddingService embeddingService;

    @Value("${embedding.dating.collection.name}")
    private String collectionName;

    @Autowired
    public DatingController(SemanticKernelService semanticKernelService, CandidateService candidateService,
            CandidateConverter candidateConverter, EmbeddingService embeddingService) {

        this.semanticKernelService = semanticKernelService;
        this.candidateService = candidateService;
        this.candidateConverter = candidateConverter;
        this.embeddingService = embeddingService;
    }

    @PostMapping("/create-collection")
    public ResponseEntity<Void> createCollection() throws ExecutionException, InterruptedException {

        embeddingService.createCollection(collectionName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check")
    public PromptResponseDto check(@RequestBody @Validated CandidateRequestDto requestDto)
            throws ExecutionException, InterruptedException {

        Candidate candidate = candidateConverter.convertToCandidate(requestDto);
        candidate = candidateService.saveOrUpdate(candidate);

        PreferenceDto preferenceDto = candidateConverter.convertToPreference(requestDto);
        candidateService.setPreference(candidate.getUsername(), preferenceDto);

        String prompt = String.format(PROMPT_TEMPLATE, candidate.getDestinyNumber(), candidate.getUsername());
        String result = semanticKernelService.getMatchInfo(prompt);
        return new PromptResponseDto(Collections.singletonList(result));
    }

    @GetMapping("/check/{id}")
    public PromptResponseDto check(@PathVariable("id") Long id) {

        Candidate candidate = candidateService.findById(id);
        PreferenceDto preferenceDto = candidateConverter.convertToPreference(candidate);
        candidateService.setPreference(candidate.getUsername(), preferenceDto);

        String prompt = String.format(PROMPT_TEMPLATE, candidate.getDestinyNumber(), candidate.getUsername());
        String result = semanticKernelService.getMatchInfo(prompt);
        return new PromptResponseDto(Collections.singletonList(result));
    }
}
