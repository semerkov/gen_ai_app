package com.epam.training.gen.ai.dating.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class PreferenceRequestDto {

    @Pattern(regexp = "^[MF]$", message = "Sex must be either M or F")
    private String sex;

    @Positive
    private Integer minAge;

    @Positive
    private Integer maxAge;
}
