package com.example.crudapp.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherDailyDto {
    private long dt; // Timestamp
    private OpenWeatherTempDto temp;
    private List<OpenWeatherWeatherDto> weather;
    private double pop; // Probability of precipitation
    private Double rain; // Rain volume, nullable

    // Getters and Setters
    public long getDt() { return dt; }
    public void setDt(long dt) { this.dt = dt; }
    public OpenWeatherTempDto getTemp() { return temp; }
    public void setTemp(OpenWeatherTempDto temp) { this.temp = temp; }
    public List<OpenWeatherWeatherDto> getWeather() { return weather; }
    public void setWeather(List<OpenWeatherWeatherDto> weather) { this.weather = weather; }
    public double getPop() { return pop; }
    public void setPop(double pop) { this.pop = pop; }
    public Double getRain() { return rain; }
    public void setRain(Double rain) { this.rain = rain; }
}
