package com.epam.training.gen.ai.dating.plugin;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.epam.training.gen.ai.dating.domain.Candidate;
import com.epam.training.gen.ai.dating.service.CandidateService;
import com.epam.training.gen.ai.service.EmbeddingService;
import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;

import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.ScoredPoint;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DatingPlugin {

    private final CandidateService candidateService;
    private final EmbeddingService embeddingService;

    @Value("${embedding.dating.collection.name}")
    private String collectionName;

    @Autowired
    public DatingPlugin(CandidateService candidateService, EmbeddingService embeddingService) {

        this.candidateService = candidateService;
        this.embeddingService = embeddingService;
    }

    @DefineKernelFunction(name = "getMatchInfo",
            description = "Get match info for candidate",
            returnDescription = "Table with information about matches",
            returnType = "java.lang.String"
    )
    public String getMatchInfo(
            @KernelFunctionParameter(description = "Candidate's username", name = "id") String username) {

        log.info("Candidate's username: {}.", username);
        if (StringUtils.isBlank(username)) {
            return "The match was not found because of error.";
        }

        try {
            List<Candidate> candidates = candidateService.findAppropriateCandidates(username);
            if (CollectionUtils.isEmpty(candidates)) {
                return "No suitable candidates were found for your profile.";
            }

            Map<Long, Float> candidatesScoreMap = calculateMatchByDescription(username, candidates);
            String response = generateResponse(candidates, candidatesScoreMap);
            log.info("Match response: {}", response);
            return response;

        } catch (Exception e) {
            String errorMsg = "The match was not found because of error.";
            log.error(errorMsg, e);
            return errorMsg;
        }
    }

    private Map<Long, Float> calculateMatchByDescription(String username, List<Candidate> candidates)
            throws ExecutionException, InterruptedException {

        String description = candidateService.findDescriptionByUsername(username);
        List<String> candidateIds = candidates.stream().map(candidate -> Long.toString(candidate.getId())).toList();
        List<Points.ScoredPoint> results = embeddingService.search(description, collectionName, candidateIds);
        return results.stream()
                .collect(Collectors.toMap(res -> res.getId().getNum(), ScoredPoint::getScore));
    }

    private String generateResponse(List<Candidate> candidates, Map<Long, Float> candidatesScoreMap) {

        StringBuilder sb = new StringBuilder("Match with the following candidates: ");
        AtomicInteger count = new AtomicInteger(0);

        candidates.stream()
                .sorted(Comparator.comparing(candidate -> candidatesScoreMap.get(candidate.getId()),
                        Comparator.reverseOrder()))
                .forEach(candidate -> sb.append(count.incrementAndGet())
                        .append(". ")
                        .append("Username: ")
                        .append(candidate.getUsername())
                        .append(". Sex: ")
                        .append(candidate.getSex().getDescription())
                        .append(". Age: ")
                        .append(Period.between(candidate.getDateOfBirth(), LocalDate.now()).getYears())
                        .append(". Destiny number: ")
                        .append(candidate.getDestinyNumber())
                        .append(". Description: ")
                        .append(candidate.getDescription())
                        .append(". Score: ")
                        .append(candidatesScoreMap.get(candidate.getId()))
                        .append(". "));

        long bestCandidateId = candidatesScoreMap.entrySet().stream()
                .max(Comparator.comparingDouble(Entry::getValue))
                .map(Entry::getKey)
                .get();
        String bestCandidateUsername = candidates.stream()
                .filter(candidate -> candidate.getId() == bestCandidateId)
                .map(Candidate::getUsername)
                .findFirst()
                .get();
        sb.append("The most suitable candidate is ")
                .append(bestCandidateUsername)
                .append(".");

        return sb.toString();
    }
}