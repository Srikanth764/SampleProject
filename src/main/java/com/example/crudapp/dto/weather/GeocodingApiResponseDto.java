package com.example.crudapp.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeocodingApiResponseDto {
    private double lat;
    private double lon;
    private String name; // Optional: for context or logging
    private String country; // Optional: for context or logging
}
