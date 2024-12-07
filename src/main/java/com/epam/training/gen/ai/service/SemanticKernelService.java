package com.epam.training.gen.ai.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.InvocationReturnMode;
import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.orchestration.ToolCallBehavior;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SemanticKernelService {

    private static final String SIMPLE_KERNEL_PREFIX = "simple_";
    private static final String CURRENCY_EXCHANGE_KERNEL_PREFIX = "currencyExchange_";

    @Value("${client-openai-deployment-name}")
    private String defaultDeploymentName;

    @Value("${client-sk-temperature:1.0}")
    private double defaultTemperature;

    @Value("${client-max-tokens:256}")
    private int defaultMaxTokens;

    private OpenAIAsyncClient openAIAsyncClient;
    private KernelPlugin kernelPlugin;
    private KernelPlugin currencyExchangeRateKernelPlugin;
    private final ChatHistory chatHistory;
    private final Map<String, Kernel> kernelMap = new ConcurrentHashMap<>();

    @Autowired
    public SemanticKernelService(OpenAIAsyncClient openAIAsyncClient, KernelPlugin currencyExchangeRateKernelPlugin,
            KernelPlugin kernelPlugin, ChatHistory chatHistory) {

        this.openAIAsyncClient = openAIAsyncClient;
        this.kernelPlugin = kernelPlugin;
        this.currencyExchangeRateKernelPlugin = currencyExchangeRateKernelPlugin;
        this.chatHistory = chatHistory;
    }

    public String processWithHistory(String input, String deploymentName, Double temperature, Integer maxTokens) {

        return processOnKernelWithHistory(kernelPlugin, input, deploymentName, temperature, maxTokens);
    }

    public String getCurrencyExchangeRate(String input, String deploymentName, Double temperature, Integer maxTokens) {

        return processOnKernelWithHistory(currencyExchangeRateKernelPlugin, input, deploymentName, temperature,
                maxTokens);
    }

    private String processOnKernelWithHistory(KernelPlugin kernelPlugin, String kernelPrefix, String input, String deploymentName,
            Double temperature, Integer maxTokens) {

        InvocationContext invocationContext = buildInvocationContext(temperature, maxTokens);

        deploymentName = StringUtils.defaultIfBlank(deploymentName, defaultDeploymentName);
        Kernel kernel = getKernel(deploymentName, kernelPlugin, kernelPrefix);

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

    private Kernel getKernel(String deploymentName, KernelPlugin kernelPlugin, String kernelPrefix) {

        return kernelMap.computeIfAbsent(deploymentName, (v) -> {
            ChatCompletionService chatCompletionService = OpenAIChatCompletion.builder()
                    .withModelId(deploymentName)
                    .withOpenAIAsyncClient(openAIAsyncClient)
                    .build();

            return Kernel.builder()
                    .withAIService(ChatCompletionService.class, chatCompletionService)
                    .withPlugin(kernelPlugin)
                    .build();
        });
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
