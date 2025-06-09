package com.example.crudapp.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherDailyDto {
    private long dt; // Timestamp
    private OpenWeatherTempDto temp;
    private List<OpenWeatherWeatherDto> weather;
    private double pop; // Probability of precipitation
    private Double rain; // Rain volume, nullable
}
