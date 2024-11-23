package com.epam.training.gen.ai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SemanticKernelService {

    private final Kernel semanticKernel;
    private final ChatHistory chatHistory;

    @Autowired
    public SemanticKernelService(Kernel semanticKernel, ChatHistory chatHistory) {

        this.semanticKernel = semanticKernel;
        this.chatHistory = chatHistory;
    }

    public String processWithHistory(String input) {

        FunctionResult<String> response = semanticKernel.invokeAsync(getChat())
                .withArguments(getKernelFunctionArguments(input, chatHistory))
                .block();

        chatHistory.addUserMessage(input);
        chatHistory.addAssistantMessage(response.getResult());
        log.info("AI answer:" + response.getResult());
        return response.getResult();

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
}
