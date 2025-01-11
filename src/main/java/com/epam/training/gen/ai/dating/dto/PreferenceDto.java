package com.epam.training.gen.ai.dating.dto;

import java.time.LocalDate;

import com.epam.training.gen.ai.dating.domain.Sex;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceDto {

    private int destinyNumber;

    private Sex sex;

    private LocalDate minDateOfBirth;

    private LocalDate maxDateOfBirth;
}
