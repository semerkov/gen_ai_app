package com.epam.training.gen.ai.config.ai;

import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Setter
@Component
@ConfigurationProperties(prefix = "model")
public class ModelConfiguration {

    private Map<String, Boolean> allowFeatures;

    public boolean isFeaturesAllowed(String modelId) {

        return ObjectUtils.defaultIfNull(allowFeatures.get(modelId), false);
    }
}
