package com.example.crudapp.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeocodingApiResponseDto {
    private double lat;
    private double lon;
    private String name; // Optional: for context or logging
    private String country; // Optional: for context or logging

    // Getters and Setters
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
