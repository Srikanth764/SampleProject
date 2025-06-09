package com.example.crudapp.service;

import com.example.crudapp.dto.weather.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.example.crudapp.exception.ApiKeyNotConfiguredException;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private final RestTemplate restTemplate;

    @Value("${openweathermap.api.key}")
    private String apiKey;

    private static final String GEOCODING_API_URL = "http://api.openweathermap.org/geo/1.0/zip";
    private static final String ONE_CALL_API_URL = "https://api.openweathermap.org/data/3.0/onecall";
    private static final String DEFAULT_COUNTRY_CODE = "US"; // Assuming US for zip codes
    private static final String DEFAULT_UNITS = "metric"; // Celsius

    public WeatherService() {
        this.restTemplate = new RestTemplate();
    }

    public List<DailyWeatherReportDto> getSevenDayForecast(String zipcode) {
        if ("YOUR_API_KEY_HERE".equals(apiKey) || apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("OpenWeatherMap API key is not configured. Please set 'openweathermap.api.key' in application.properties.");
            throw new ApiKeyNotConfiguredException("OpenWeatherMap API key is not configured. Please set 'openweathermap.api.key' in application.properties.");
        }

        try {
            GeocodingApiResponseDto geoResponse = getCoordinates(zipcode);
            if (geoResponse == null) { // Or check for empty lat/lon if the DTO can be non-null but empty
                 logger.warn("Could not retrieve coordinates for zipcode: {}", zipcode);
                // Controller should handle this by returning 400 or 404 based on this outcome
                throw new RuntimeException("Invalid zipcode or unable to geocode.");
            }

            OpenWeatherOneCallResponseDto weatherData = fetchWeatherForecast(geoResponse.getLat(), geoResponse.getLon(), DEFAULT_UNITS);
            if (weatherData == null || weatherData.getDaily() == null || weatherData.getDaily().isEmpty()) {
                logger.warn("No daily weather data received from OpenWeatherMap for lat: {}, lon: {}", geoResponse.getLat(), geoResponse.getLon());
                return Collections.emptyList();
            }
            return mapToDailyWeatherReportDto(weatherData, DEFAULT_UNITS);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling OpenWeatherMap API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            // Rethrow a more specific application exception or let controller handle generic one
            throw new RuntimeException("Error fetching weather data: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred in WeatherService for zipcode {}: {}", zipcode, e.getMessage(), e);
            throw new RuntimeException("Unexpected error processing weather request: " + e.getMessage(), e);
        }
    }

    private GeocodingApiResponseDto getCoordinates(String zipcode) {
        // The OpenWeatherMap Geocoding API documentation for zip codes indicates it can return an array
        // for city name searches, but for zip code, it returns a single object.
        // If it could return an array for zip, the DTO type here would be GeocodingApiResponseDto[].class
        // and we'd take the first element if the array is not empty.
        // Based on example: http://api.openweathermap.org/geo/1.0/zip?zip=E14,GB&appid={API key} -> returns single object.

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(GEOCODING_API_URL)
                .queryParam("zip", zipcode + "," + DEFAULT_COUNTRY_CODE)
                .queryParam("appid", apiKey);

        logger.info("Fetching coordinates for zipcode: {} from URL: {}", zipcode, uriBuilder.toUriString());
        try {
            // Try fetching as a single object first, as per zip code API example
            GeocodingApiResponseDto response = restTemplate.getForObject(uriBuilder.toUriString(), GeocodingApiResponseDto.class);
            if (response != null && response.getLat() != 0 && response.getLon() != 0) { // Basic check
                logger.info("Successfully fetched coordinates: lat={}, lon={} for zipcode {}", response.getLat(), response.getLon(), zipcode);
                return response;
            } else {
                 // Attempt to parse as array if single object parsing fails or returns null/empty
                try {
                    GeocodingApiResponseDto[] arrayResponse = restTemplate.getForObject(uriBuilder.toUriString(), GeocodingApiResponseDto[].class);
                    if (arrayResponse != null && arrayResponse.length > 0 && arrayResponse[0].getLat() != 0 && arrayResponse[0].getLon() != 0) {
                        logger.info("Successfully fetched coordinates (from array): lat={}, lon={} for zipcode {}", arrayResponse[0].getLat(), arrayResponse[0].getLon(), zipcode);
                        return arrayResponse[0];
                    }
                } catch (Exception arrayEx) {
                    logger.warn("Failed to parse geocoding response as array for zipcode {}: {}", zipcode, arrayEx.getMessage());
                }
                logger.warn("Geocoding response for zipcode {} was null, empty, or lacked coordinates.", zipcode);
                return null; // Or throw specific exception
            }
        } catch (HttpClientErrorException e) {
            logger.error("Client error during geocoding for zipcode {}: {} - {}", zipcode, e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 404) { // Not Found by OpenWeatherMap often means invalid zip
                 throw new RuntimeException("Zipcode not found or invalid: " + zipcode, e);
            }
            throw e; // Re-throw other client errors
        }
    }

    private OpenWeatherOneCallResponseDto fetchWeatherForecast(double lat, double lon, String units) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(ONE_CALL_API_URL)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("exclude", "current,minutely,hourly,alerts")
                .queryParam("units", units)
                .queryParam("appid", apiKey);

        logger.info("Fetching weather forecast for lat: {}, lon: {} from URL: {}", lat, lon, uriBuilder.toUriString());
        OpenWeatherOneCallResponseDto response = restTemplate.getForObject(uriBuilder.toUriString(), OpenWeatherOneCallResponseDto.class);
        logger.info("Successfully fetched weather forecast.");
        return response;
    }

    private List<DailyWeatherReportDto> mapToDailyWeatherReportDto(OpenWeatherOneCallResponseDto weatherData, String unitsSystem) {
        List<DailyWeatherReportDto> reports = new ArrayList<>();
        String temperatureUnit = "metric".equalsIgnoreCase(unitsSystem) ? "Celsius" : ("imperial".equalsIgnoreCase(unitsSystem) ? "Fahrenheit" : "Kelvin");

        if (weatherData.getDaily() != null) {
            for (int i = 0; i < weatherData.getDaily().size() && i < 7; i++) { // Max 7 days
                OpenWeatherDailyDto dailyDto = weatherData.getDaily().get(i);
                DailyWeatherReportDto report = new DailyWeatherReportDto();

                LocalDate date = Instant.ofEpochSecond(dailyDto.getDt()).atZone(ZoneOffset.UTC).toLocalDate();
                report.setDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE));

                if (dailyDto.getTemp() != null) {
                    report.setMinTemperature(dailyDto.getTemp().getMin());
                    report.setMaxTemperature(dailyDto.getTemp().getMax());
                }

                if (dailyDto.getWeather() != null && !dailyDto.getWeather().isEmpty()) {
                    report.setWeatherDescription(dailyDto.getWeather().get(0).getDescription());
                } else {
                    report.setWeatherDescription("N/A");
                }
                report.setRainProbability(dailyDto.getPop());
                report.setTemperatureUnit(temperatureUnit);
                reports.add(report);
            }
        }
        return reports;
    }
}
