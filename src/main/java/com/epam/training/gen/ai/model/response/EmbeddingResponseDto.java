package com.epam.training.gen.ai.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class EmbeddingResponseDto {

    private String id;
    private String text;
    private Float score;
}
