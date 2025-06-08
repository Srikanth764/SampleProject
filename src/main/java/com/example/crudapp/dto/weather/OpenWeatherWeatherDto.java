package com.example.crudapp.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherWeatherDto {
    private String description;
    private String main; // e.g., "Rain", "Clouds"

    // Getters and Setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMain() { return main; }
    public void setMain(String main) { this.main = main; }
}
