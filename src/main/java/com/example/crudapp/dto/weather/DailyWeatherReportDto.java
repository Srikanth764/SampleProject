package com.example.crudapp.dto.weather;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyWeatherReportDto {
    private String date; // Format: YYYY-MM-DD
    private double minTemperature;
    private double maxTemperature;
    private String weatherDescription;
    private double rainProbability; // 0.0 to 1.0
    private String temperatureUnit; // e.g., "Celsius", "Fahrenheit"
}
