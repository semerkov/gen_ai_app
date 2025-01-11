package com.epam.training.gen.ai.dating.converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.epam.training.gen.ai.dating.domain.Candidate;
import com.epam.training.gen.ai.dating.domain.Sex;
import com.epam.training.gen.ai.dating.dto.PreferenceDto;
import com.epam.training.gen.ai.dating.dto.request.CandidateRequestDto;
import com.epam.training.gen.ai.dating.dto.request.PreferenceRequestDto;

@Component
public class CandidateConverter {

    /**
     * The best combinations of destiny numbers add up to the number 10.
     *
     * @see <a href="https://instaastro.com/numerology/destiny-number/">Destiny Number Calculator | InstaAstro Astrology</a>
     */
    private static final int SUM_OF_DESTINY_NUMBERS = 10;

    private static final int DEFAULT_AGE_DIFFERENCE = 3;

    public Candidate convertToCandidate(CandidateRequestDto requestDto) {

        return Candidate.builder()
                .username(requestDto.getUsername())
                .dateOfBirth(requestDto.getDateOfBirth())
                .sex(Sex.valueOf(requestDto.getSex()))
                .destinyNumber(calculateDestinyNumber(requestDto.getDateOfBirth()))
                .description(requestDto.getDescription())
                .build();
    }

    public PreferenceDto convertToPreference(CandidateRequestDto requestDto) {

        Sex sex = null;
        LocalDate minDateOfBirth = null;
        LocalDate maxDateOfBirth = null;

        PreferenceRequestDto preferenceRequestDto = requestDto.getPreference();
        if (preferenceRequestDto != null) {
            if (preferenceRequestDto.getSex() != null) {
                sex = Sex.valueOf(preferenceRequestDto.getSex());
            }

            if (preferenceRequestDto.getMaxAge() != null) {
                minDateOfBirth = LocalDate.now().minusYears(preferenceRequestDto.getMaxAge());
            }

            if (preferenceRequestDto.getMinAge() != null) {
                maxDateOfBirth = LocalDate.now().minusYears(preferenceRequestDto.getMinAge());
            }
        }

        if (sex == null) {
            sex = Sex.M.name().equals(requestDto.getSex()) ? Sex.F : Sex.M;
        }

        if (minDateOfBirth == null) {
            minDateOfBirth = requestDto.getDateOfBirth().minusYears(DEFAULT_AGE_DIFFERENCE);
        }

        if (maxDateOfBirth == null) {
            maxDateOfBirth = requestDto.getDateOfBirth().plusYears(DEFAULT_AGE_DIFFERENCE);
        }

        return PreferenceDto.builder()
                .destinyNumber(SUM_OF_DESTINY_NUMBERS - calculateDestinyNumber(requestDto.getDateOfBirth()))
                .sex(sex)
                .minDateOfBirth(minDateOfBirth)
                .maxDateOfBirth(maxDateOfBirth)
                .build();
    }

    public PreferenceDto convertToPreference(Candidate candidate) {

        return PreferenceDto.builder()
                .destinyNumber(SUM_OF_DESTINY_NUMBERS - candidate.getDestinyNumber())
                .sex(Sex.M == candidate.getSex() ? Sex.F : Sex.M)
                .minDateOfBirth(candidate.getDateOfBirth().minusYears(DEFAULT_AGE_DIFFERENCE))
                .maxDateOfBirth(candidate.getDateOfBirth().plusYears(DEFAULT_AGE_DIFFERENCE))
                .build();
    }

    private int calculateDestinyNumber(LocalDate date) {

        // Formats as YYYYMMDD
        String dateValue = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        return reduceToSingleDigit(dateValue);
    }

    private int reduceToSingleDigit(String dateValue) {

        int sum = dateValue.chars()
                .filter(Character::isDigit)
                .map(Character::getNumericValue)
                .sum();
        while (sum > 9) {
            sum = reduceToSingleDigit(String.valueOf(sum));
        }
        return sum;
    }
}
