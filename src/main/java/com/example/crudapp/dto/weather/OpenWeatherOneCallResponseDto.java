package com.example.crudapp.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherOneCallResponseDto {
    private List<OpenWeatherDailyDto> daily;

    // Getters and Setters
    public List<OpenWeatherDailyDto> getDaily() { return daily; }
    public void setDaily(List<OpenWeatherDailyDto> daily) { this.daily = daily; }
}
