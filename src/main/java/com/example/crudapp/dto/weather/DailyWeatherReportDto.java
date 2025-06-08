package com.example.crudapp.dto.weather;

// No Jackson annotations needed here unless it's also used for incoming requests
public class DailyWeatherReportDto {
    private String date; // Format: YYYY-MM-DD
    private double minTemperature;
    private double maxTemperature;
    private String weatherDescription;
    private double rainProbability; // 0.0 to 1.0
    private String temperatureUnit; // e.g., "Celsius", "Fahrenheit"

    // Constructors (optional, but can be useful)
    public DailyWeatherReportDto() {}

    public DailyWeatherReportDto(String date, double minTemperature, double maxTemperature, String weatherDescription, double rainProbability, String temperatureUnit) {
        this.date = date;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.weatherDescription = weatherDescription;
        this.rainProbability = rainProbability;
        this.temperatureUnit = temperatureUnit;
    }

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public double getMinTemperature() { return minTemperature; }
    public void setMinTemperature(double minTemperature) { this.minTemperature = minTemperature; }
    public double getMaxTemperature() { return maxTemperature; }
    public void setMaxTemperature(double maxTemperature) { this.maxTemperature = maxTemperature; }
    public String getWeatherDescription() { return weatherDescription; }
    public void setWeatherDescription(String weatherDescription) { this.weatherDescription = weatherDescription; }
    public double getRainProbability() { return rainProbability; }
    public void setRainProbability(double rainProbability) { this.rainProbability = rainProbability; }
    public String getTemperatureUnit() { return temperatureUnit; }
    public void setTemperatureUnit(String temperatureUnit) { this.temperatureUnit = temperatureUnit; }
}
