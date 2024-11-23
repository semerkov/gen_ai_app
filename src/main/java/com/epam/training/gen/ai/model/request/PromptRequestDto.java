package com.epam.training.gen.ai.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PromptRequestDto {

    @NotNull
    @NotEmpty
    private String input;
}
