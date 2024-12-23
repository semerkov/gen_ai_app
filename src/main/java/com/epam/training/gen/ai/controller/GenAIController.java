package com.epam.training.gen.ai.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epam.training.gen.ai.model.request.PromptRequestDto;
import com.epam.training.gen.ai.model.response.PromptResponseDto;
import com.epam.training.gen.ai.service.OpenAIService;
import com.epam.training.gen.ai.service.SemanticKernelService;

@RestController
@RequestMapping("/prompt")
public class GenAIController {

    private final OpenAIService openAIService;
    private final SemanticKernelService semanticKernelService;

    @Autowired
    public GenAIController(OpenAIService openAIService, SemanticKernelService semanticKernelService) {

        this.openAIService = openAIService;
        this.semanticKernelService = semanticKernelService;
    }

    @PostMapping("/open-ai/send")
    public PromptResponseDto sendPromptToOpenAI(@Validated @RequestBody PromptRequestDto request) {

        List<String> result = openAIService.getChatCompletions(request.getInput());
        return new PromptResponseDto(result);
    }

    @PostMapping("/sk/send")
    public PromptResponseDto sendPromptToSK(
            @RequestHeader(name = "deploymentName", required = false) String deploymentName,
            @RequestHeader(name = "temperature", required = false) Double temperature,
            @RequestHeader(name = "maxTokens", required = false) Integer maxTokens,
            @Validated @RequestBody PromptRequestDto request) {

        String result = semanticKernelService.processWithHistory(request.getInput(), deploymentName, temperature,
                maxTokens);
        return new PromptResponseDto(Collections.singletonList(result));
    }

    @PostMapping("/sk/place/commonInfo")
    public PromptResponseDto getCurrencyExchangeRate(
            @RequestHeader(name = "deploymentName", required = false) String deploymentName,
            @RequestHeader(name = "temperature", required = false) Double temperature,
            @RequestHeader(name = "maxTokens", required = false) Integer maxTokens,
            @Validated @RequestBody PromptRequestDto request) {

        String result = semanticKernelService.getCommonInfoAboutPlace(request.getInput(), deploymentName, temperature,
                maxTokens);
        return new PromptResponseDto(Collections.singletonList(result));
    }
}
