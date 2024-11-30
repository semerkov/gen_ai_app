package com.epam.training.gen.ai.service;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.InvocationReturnMode;
import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.orchestration.ToolCallBehavior;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SemanticKernelService {

    @Value("${client-sk-temperature:1.0}")
    private double defaultTemperature;

    @Value("${client-max-tokens:256}")
    private int defaultMaxTokens;

    private final Kernel semanticKernel;
    private final Kernel currencyExchangeRateKernel;
    private final ChatHistory chatHistory;

    @Autowired
    public SemanticKernelService(Kernel semanticKernel, Kernel currencyExchangeRateKernel, ChatHistory chatHistory) {

        this.semanticKernel = semanticKernel;
        this.currencyExchangeRateKernel = currencyExchangeRateKernel;
        this.chatHistory = chatHistory;
    }

    public String processWithHistory(String input, Double temperature, Integer maxTokens) {

        return processOnKernelWithHistory(semanticKernel, input, temperature, maxTokens);
    }

    public String getCurrencyExchangeRate(String input, Double temperature, Integer maxTokens) {

        return processOnKernelWithHistory(currencyExchangeRateKernel, input, temperature, maxTokens);
    }

    private String processOnKernelWithHistory(Kernel kernel, String input, Double temperature, Integer maxTokens) {

        InvocationContext invocationContext = buildInvocationContext(temperature, maxTokens);
        FunctionResult<String> response = kernel.invokeAsync(getChat())
                .withArguments(getKernelFunctionArguments(input, chatHistory))
                .withInvocationContext(invocationContext)
                .block();

        chatHistory.addUserMessage(input);

        String assistantMessage = response.getResult();
        chatHistory.addAssistantMessage(assistantMessage);
        log.info("AI answer: {}", assistantMessage);
        return assistantMessage;
    }

    /**
     * Creates a kernel function for generating a chat response using a predefined prompt template.
     * <p>
     * The template includes the chat history and the user's message as variables.
     *
     * @return a {@link KernelFunction} for handling chat-based AI interactions
     */

    private KernelFunction<String> getChat() {

        return KernelFunction.<String>createFromPrompt("""
                        {{$chatHistory}}
                        <message role="user">{{$input}}</message>""")
                .build();
    }


    /**
     * Creates the kernel function arguments with the user prompt and chat history.
     *
     * @param input       the user's input
     * @param chatHistory the current chat history
     * @return a {@link KernelFunctionArguments} instance containing the variables for the AI model
     */

    private KernelFunctionArguments getKernelFunctionArguments(String input, ChatHistory chatHistory) {

        return KernelFunctionArguments.builder()
                .withVariable("input", input)
                .withVariable("chatHistory", chatHistory)
                .build();
    }

    private InvocationContext buildInvocationContext(Double temperature, Integer maxTokens) {

        temperature = ObjectUtils.defaultIfNull(temperature, defaultTemperature);
        maxTokens = ObjectUtils.defaultIfNull(maxTokens, defaultMaxTokens);
        log.info("Temperature = {}; maxTokens = {}.", temperature, maxTokens);

        PromptExecutionSettings promptExecutionSettings = PromptExecutionSettings.builder()
                .withTemperature(temperature)
                .withMaxTokens(maxTokens)
                .build();

        return InvocationContext.builder()
                .withReturnMode(InvocationReturnMode.LAST_MESSAGE_ONLY)
                .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
                .withPromptExecutionSettings(promptExecutionSettings)
                .build();
    }
}
