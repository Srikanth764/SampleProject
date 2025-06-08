package com.example.crudapp.controller;

import com.example.crudapp.dto.weather.DailyWeatherReportDto;
import com.example.crudapp.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.example.crudapp.exception.ApiKeyNotConfiguredException;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);
    private final WeatherService weatherService;

    @Autowired
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/{zipcode}")
    public ResponseEntity<?> getWeatherForecast(@PathVariable String zipcode) {
        logger.info("Received weather forecast request for zipcode: {}", zipcode);

        // Basic US zipcode validation
        if (zipcode == null || !zipcode.matches("^\\d{5}$")) {
            logger.warn("Invalid zipcode format received: {}", zipcode);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid zipcode format. Must be 5 digits for US."));
        }

        try {
            List<DailyWeatherReportDto> forecast = weatherService.getSevenDayForecast(zipcode);
            // If service returns an empty list without throwing an exception (e.g. OWM had no data), this is OK.
            logger.info("Successfully retrieved weather forecast for zipcode: {}", zipcode);
            return ResponseEntity.ok(forecast);
        } catch (ApiKeyNotConfiguredException e) {
            logger.error("API key not configured: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Server configuration error: API key for weather service is not set."));
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (message.contains("zipcode not found") || message.contains("invalid zipcode")) {
                logger.warn("Failed to get weather for zipcode {}: {}", zipcode, e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            } else if (message.contains("error fetching weather data") || e.getCause() instanceof HttpClientErrorException || e.getCause() instanceof HttpServerErrorException) {
                logger.error("External weather service error for zipcode {}: {}", zipcode, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "External weather service is currently unavailable or returned an error."));
            } else { // Generic catch-all for other RuntimeExceptions
                logger.error("Unexpected error processing weather request for zipcode {}: {}", zipcode, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected internal error occurred."));
            }
        }
    }
}
