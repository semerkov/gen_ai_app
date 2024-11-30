package com.epam.training.gen.ai.plugin.currencyExchangeRate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CurrencyExchangeRatePlugin {

    private final NbpClient nbpClient;

    @Autowired
    public CurrencyExchangeRatePlugin(NbpClient nbpClient) {

        this.nbpClient = nbpClient;
    }

    @DefineKernelFunction(name = "getCurrencyRate",
            description = "Get last currency rate",
            returnDescription = "The cost of one unit of foreign currency in Polish zloty",
            returnType = "java.lang.String"
    )
    public String getCurrencyRate(
            @KernelFunctionParameter(
                    description = "A three-letter code (ISO 4217 standard) of currency whose exchange rate should be find out",
                    name = "currencyCode") String currencyCode) {

        log.info("Currency code: {}.", currencyCode);
        if (StringUtils.isBlank(currencyCode)) {
            return "Exchange rate was not found for currency with code " + currencyCode;
        }

        try {
            CurrencyExchangeRateResponseDto responseDto = nbpClient.getExchangeRate(currencyCode);
            log.info("NBP REST response: {}", responseDto);

            CurrencyExchangeItemResponseDto rateItem = responseDto.getRates().get(0);
            return new StringBuilder("1 ")
                    .append(responseDto.getCode())
                    .append(" = ")
                    .append(responseDto.getRates().get(0).getMid())
                    .append(" PLN (")
                    .append(rateItem.getEffectiveDate())
                    .append(")")
                    .toString();
        } catch (Exception e) {
            String errorMsg = "Exchange rate was not found for currency with code " + currencyCode + ".";
            log.error(errorMsg, e);
            return errorMsg;
        }
    }
}
