package com.epam.training.gen.ai.dating.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CandidateRequestDto {

    @NotBlank
    @Size(max = 255)
    private String username;

    @NotNull
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate dateOfBirth;

    @NotBlank
    @Pattern(regexp = "^[MF]$", message = "Sex must be either M or F")
    private String sex;

    @NotBlank
    @Size(max = 1000)
    private String description;

    @Valid
    private PreferenceRequestDto preference;
}
