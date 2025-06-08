package com.example.crudapp.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherTempDto {
    private double min;
    private double max;

    // Getters and Setters
    public double getMin() { return min; }
    public void setMin(double min) { this.min = min; }
    public double getMax() { return max; }
    public void setMax(double max) { this.max = max; }
}
