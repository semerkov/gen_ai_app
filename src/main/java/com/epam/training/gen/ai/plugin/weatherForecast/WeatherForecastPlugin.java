package com.epam.training.gen.ai.plugin.weatherForecast;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.epam.training.gen.ai.plugin.weatherForecast.WeatherForecastResponseDto.Hourly;
import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WeatherForecastPlugin {

    private final WeatherForecastClient weatherForecastClient;

    @Autowired
    public WeatherForecastPlugin(WeatherForecastClient weatherForecastClient) {

        this.weatherForecastClient = weatherForecastClient;
    }

    @DefineKernelFunction(name = "getWeatherForecast",
            description = "Get weather forecast by city's geographical latitude and longitude. Use country's capital geographical coordinates if city's name is not specified",
            returnDescription = "Air temperature at certain points in time",
            returnType = "java.lang.String"
    )
    public String getWeatherForecast(
            @KernelFunctionParameter(description = "Name of the city",
                    name = "city") String city,
            @KernelFunctionParameter(description = "City's latitude in decimal format. Geographical WGS84 coordinates of the location are used.",
                    name = "latitude") Double latitude,
            @KernelFunctionParameter(description = "City's longitude in decimal format. Geographical WGS84 coordinates of the location are used.",
                    name = "longitude") Double longitude) {

        log.info("City: {}, latitude: {}, longitude: {}.", city, latitude, longitude);
        if (latitude == null || longitude == null) {
            return MessageFormat.format("Weather forecast was not found for {0} with latitude: {1} and longitude: {2}.",
                    city, latitude, longitude);
        }

        try {
            WeatherForecastResponseDto responseDto = weatherForecastClient.getWeatherForecast(latitude, longitude);
            log.info("Open-meteo REST response: {}", responseDto);

            Hourly hourly = responseDto.getHourly();
            if (!CollectionUtils.isEmpty(hourly.getTime())) {
                List<Long> timePoints = hourly.getTime();
                List<Double> temperatures = hourly.getTemperature2m();
                String temperatureSymbol = responseDto.getHourlyUnits().getTemperature2m();
                StringBuilder sb = new StringBuilder("City: ")
                        .append(city)
                        .append(StringUtils.SPACE)
                        .append("Current date: ")
                        .append(LocalDate.now())
                        .append(StringUtils.SPACE);

                for (int i = 0; i < timePoints.size(); i++) {
                    Instant instant = Instant.ofEpochSecond(timePoints.get(i));
                    LocalDateTime localDateTime = instant.atZone(ZoneId.of("CET")).toLocalDateTime();

                    sb.append("Time: ")
                            .append(localDateTime)
                            .append(StringUtils.SPACE)
                            .append("temperature: ")
                            .append(temperatures.get(i))
                            .append(temperatureSymbol)
                            .append(";")
                            .append(StringUtils.SPACE);
                }

                return sb.toString();
            }

            throw new IllegalStateException("No results.");
        } catch (Exception e) {
            String errorMsg = MessageFormat.format(
                    "Weather forecast was not found for {0} with latitude: {1} and longitude: {2}.",
                    city, latitude, longitude);
            log.error(errorMsg, e);
            return errorMsg;
        }
    }
}
