package com.epam.training.gen.ai.plugin.weatherForecast;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Weather forecast web client.
 *
 * @see <a href="https://open-meteo.com/en/docs">Docs | Open-Meteo.com</a>
 */
@Component
public class WeatherForecastClient {

    private final WebClient webClient;

    public WeatherForecastClient() {

        this.webClient = WebClient.builder()
                .baseUrl("https://api.open-meteo.com")
                .build();
    }

    public WeatherForecastResponseDto getWeatherForecast(Double latitude, Double longitude) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly", "temperature_2m")
                        .queryParam("format", "json")
                        .queryParam("timeformat", "unixtime")
                        .queryParam("timezone", "CET")
                        .build())
                .retrieve()
                .bodyToMono(WeatherForecastResponseDto.class)
                .block();
    }
}
