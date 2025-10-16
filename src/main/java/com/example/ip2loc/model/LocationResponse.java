package com.example.ip2loc.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing the relevant fields returned from the IP2Location database.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocationResponse(
        String ip,
        String countryCode,
        String countryName,
        String regionName,
        String cityName,
        String zipCode,
        Double latitude,
        Double longitude,
        String timeZone,
        String isp) {
}
