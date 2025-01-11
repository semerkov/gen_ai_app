package com.epam.training.gen.ai.dating.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.epam.training.gen.ai.dating.domain.Candidate;
import com.epam.training.gen.ai.dating.domain.Sex;

@Repository
public interface CandidateRepository extends CrudRepository<Candidate, Long> {

    Candidate getByUsername(String username);

    @Query("SELECT description FROM Candidate WHERE username = ?1")
    String findDescriptionByUsername(String username);

    Page<Candidate> findByDestinyNumberAndUsernameNotAndSexAndDateOfBirthBetween(int destinyNumber, String username,
            Sex sex, LocalDate minDateOfBirth, LocalDate maxDateOfBirth, Pageable pageable);
}
