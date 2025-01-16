package com.epam.training.gen.ai.dating.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.epam.training.gen.ai.dating.plugin.DatingPlugin;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;

@Configuration
public class DatingSKConfiguration {

    @Bean
    public KernelPlugin datingKernelPlugin(DatingPlugin datingPlugin) {

        return KernelPluginFactory.createFromObject(datingPlugin, "Dating_Plugin");
    }

    @Bean
    public Kernel datingKernel(ChatCompletionService defaultChatCompletionService,
            KernelPlugin datingKernelPlugin) {

        return Kernel.builder()
                .withAIService(ChatCompletionService.class, defaultChatCompletionService)
                .withPlugin(datingKernelPlugin)
                .build();
    }
}
