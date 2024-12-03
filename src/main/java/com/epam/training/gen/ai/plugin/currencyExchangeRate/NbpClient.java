package com.epam.training.gen.ai.plugin.currencyExchangeRate;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The National Bank of Poland web client.
 *
 * @see <a href="https://api.nbp.pl/en.html">National Bank of Poland Web API</a>
 */
@Component
public class NbpClient {

    private final WebClient webClient;

    public NbpClient() {

        this.webClient = WebClient.builder()
                .baseUrl("https://api.nbp.pl")
                .build();
    }

    public CurrencyExchangeRateResponseDto getExchangeRate(String currencyCode) {

        return webClient.get()
                .uri("/api/exchangerates/rates/a/{currencyCode}?format=json", Map.of("currencyCode", currencyCode))
                .retrieve()
                .bodyToMono(CurrencyExchangeRateResponseDto.class)
                .block();
    }
}
