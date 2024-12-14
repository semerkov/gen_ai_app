package com.epam.training.gen.ai.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.epam.training.gen.ai.config.ai.ModelConfiguration;
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

    private static final String SIMPLE_KERNEL_PREFIX = "simple";
    private static final String CURRENCY_EXCHANGE_KERNEL_PREFIX = "currencyExchange";

    @Value("${client-openai-deployment-name}")
    private String defaultDeploymentName;

    @Value("${client-sk-temperature:1.0}")
    private double defaultTemperature;

    @Value("${client-max-tokens:256}")
    private int defaultMaxTokens;

    private final ModelConfiguration modelConfiguration;
    private final OpenAIAsyncClient openAIAsyncClient;
    private final KernelPlugin kernelPlugin;
    private final KernelPlugin currencyExchangeRateKernelPlugin;
    private final KernelPlugin weatherForecastKernelPlugin;
    private final ChatHistory chatHistory;
    private final Map<String, Kernel> kernelMap = new ConcurrentHashMap<>();

    @Autowired
    public SemanticKernelService(ModelConfiguration modelConfiguration, OpenAIAsyncClient openAIAsyncClient,
            KernelPlugin currencyExchangeRateKernelPlugin, KernelPlugin weatherForecastKernelPlugin,
            KernelPlugin kernelPlugin, ChatHistory chatHistory) {

        this.modelConfiguration = modelConfiguration;
        this.openAIAsyncClient = openAIAsyncClient;
        this.kernelPlugin = kernelPlugin;
        this.currencyExchangeRateKernelPlugin = currencyExchangeRateKernelPlugin;
        this.weatherForecastKernelPlugin = weatherForecastKernelPlugin;
        this.chatHistory = chatHistory;
    }

    public String processWithHistory(String input, String deploymentName, Double temperature, Integer maxTokens) {

        return processOnKernelWithHistory(Collections.singletonList(kernelPlugin), SIMPLE_KERNEL_PREFIX, input,
                deploymentName, temperature,
                maxTokens);
    }

    public String getCommonInfoAboutPlace(String input, String deploymentName, Double temperature, Integer maxTokens) {

        return processOnKernelWithHistory(List.of(currencyExchangeRateKernelPlugin, weatherForecastKernelPlugin),
                CURRENCY_EXCHANGE_KERNEL_PREFIX, input, deploymentName, temperature, maxTokens);
    }

    private String processOnKernelWithHistory(List<KernelPlugin> kernelPlugins, String kernelPrefix, String input,
            String deploymentName, Double temperature, Integer maxTokens) {

        deploymentName = StringUtils.defaultIfBlank(deploymentName, defaultDeploymentName);
        InvocationContext invocationContext = buildInvocationContext(deploymentName, temperature, maxTokens);
        Kernel kernel = getKernel(deploymentName, kernelPlugins, kernelPrefix);
        log.info("Deployment name: {}, temperature: {}, max tokens: {}.", deploymentName, temperature, maxTokens);

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

    private Kernel getKernel(String deploymentName, List<KernelPlugin> kernelPlugins, String kernelPrefix) {

        String kernelKey = kernelPrefix + "_" + deploymentName;
        return kernelMap.computeIfAbsent(kernelKey, (v) -> {
            ChatCompletionService chatCompletionService = OpenAIChatCompletion.builder()
                    .withModelId(deploymentName)
                    .withOpenAIAsyncClient(openAIAsyncClient)
                    .build();

            Kernel.Builder kernelBuilder = Kernel.builder()
                    .withAIService(ChatCompletionService.class, chatCompletionService);

            if (!CollectionUtils.isEmpty(kernelPlugins)) {
                kernelPlugins.forEach(kernelBuilder::withPlugin);
            }

            return kernelBuilder.build();
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

    private InvocationContext buildInvocationContext(String deploymentName, Double temperature, Integer maxTokens) {

        temperature = ObjectUtils.defaultIfNull(temperature, defaultTemperature);
        maxTokens = ObjectUtils.defaultIfNull(maxTokens, defaultMaxTokens);

        PromptExecutionSettings promptExecutionSettings = PromptExecutionSettings.builder()
                .withTemperature(temperature)
                .withMaxTokens(maxTokens)
                .build();

        boolean isFeaturesAllowed = modelConfiguration.isFeaturesAllowed(deploymentName);
        ToolCallBehavior toolCallBehavior = isFeaturesAllowed ? ToolCallBehavior.allowAllKernelFunctions(true) : null;
        return InvocationContext.builder()
                .withReturnMode(InvocationReturnMode.LAST_MESSAGE_ONLY)
                .withToolCallBehavior(toolCallBehavior)
                .withPromptExecutionSettings(promptExecutionSettings)
                .build();
    }
}
