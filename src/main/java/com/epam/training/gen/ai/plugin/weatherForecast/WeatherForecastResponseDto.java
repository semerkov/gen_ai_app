package com.epam.training.gen.ai.plugin.weatherForecast;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

@Data
public class WeatherForecastResponseDto {

    private double latitude;

    private double longitude;

    @JsonAlias("generationtime_ms")
    private double generationtimeMs;

    @JsonAlias("utc_offset_seconds")
    private int utcOffsetSeconds;

    private String timezone;

    @JsonAlias("timezone_abbreviation")
    private String timezoneAbbreviation;

    private double elevation;

    @JsonAlias("hourly_units")

    private HourlyUnits hourlyUnits;
    private Hourly hourly;

    @Data
    public static class HourlyUnits {

        private String time;

        @JsonAlias("temperature_2m")
        private String temperature2m;
    }

    @Data
    public static class Hourly {

        private List<Long> time;

        @JsonAlias("temperature_2m")
        private List<Double> temperature2m;
    }
}