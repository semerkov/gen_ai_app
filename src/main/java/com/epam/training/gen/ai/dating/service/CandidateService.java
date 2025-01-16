package com.epam.training.gen.ai.dating.service;

import static io.qdrant.client.PointIdFactory.id;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.epam.training.gen.ai.dating.domain.Candidate;
import com.epam.training.gen.ai.dating.dto.PreferenceDto;
import com.epam.training.gen.ai.dating.repository.CandidateRepository;
import com.epam.training.gen.ai.service.EmbeddingService;

@Service
public class CandidateService {

    private static final int FIRST_PAGE = 0;
    private static final int PAGE_SIZE = 10;

    private final CandidateRepository candidateRepository;
    private final EmbeddingService embeddingService;
    private final Map<String, PreferenceDto> candidatePreferences = new ConcurrentHashMap<>();

    @Value("${embedding.dating.collection.name}")
    private String collectionName;

    @Autowired
    public CandidateService(CandidateRepository candidateRepository, EmbeddingService embeddingService) {

        this.candidateRepository = candidateRepository;
        this.embeddingService = embeddingService;
    }

    public Candidate findById(long id) {

        return candidateRepository.findById(id).orElse(null);
    }

    public String findDescriptionByUsername(String username) {

        return candidateRepository.findDescriptionByUsername(username);
    }

    public Candidate saveOrUpdate(Candidate candidate) throws ExecutionException, InterruptedException {

        Candidate existingCandidate = candidateRepository.getByUsername(candidate.getUsername());
        if (existingCandidate != null) {
            existingCandidate.setSex(candidate.getSex());
            existingCandidate.setDateOfBirth(candidate.getDateOfBirth());
            existingCandidate.setDestinyNumber(candidate.getDestinyNumber());
            candidate = candidateRepository.save(existingCandidate);
        }

        candidate = candidateRepository.save(candidate);
        embeddingService.processAndSaveText(id(candidate.getId()), candidate.getDescription(), collectionName);
        return candidate;
    }

    public void setPreference(String username, PreferenceDto preferenceDto) {

        candidatePreferences.put(username, preferenceDto);
    }

    public List<Candidate> findAppropriateCandidates(String username) {

        PreferenceDto preferenceDto = candidatePreferences.remove(username);
        if (preferenceDto == null) {
            throw new IllegalStateException("Cannot find preference for candidate with username = " + username + ".");
        }

        Pageable pageable = PageRequest.of(FIRST_PAGE, PAGE_SIZE);
        Page<Candidate> candidatePageList = candidateRepository.findByDestinyNumberAndUsernameNotAndSexAndDateOfBirthBetween(
                preferenceDto.getDestinyNumber(),
                username,
                preferenceDto.getSex(),
                preferenceDto.getMinDateOfBirth(),
                preferenceDto.getMaxDateOfBirth(),
                pageable);

        return candidatePageList.getContent();
    }
}
