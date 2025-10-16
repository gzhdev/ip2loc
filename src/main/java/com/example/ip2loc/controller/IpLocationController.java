package com.example.ip2loc.controller;

import com.example.ip2loc.model.LocationResponse;
import com.example.ip2loc.service.Ip2LocationService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes IP geolocation lookups.
 */
@RestController
@RequestMapping("/api/location")
public class IpLocationController {

    private static final Logger log = LoggerFactory.getLogger(IpLocationController.class);

    private final Ip2LocationService locationService;

    public IpLocationController(Ip2LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public ResponseEntity<?> locate(@RequestParam(name = "ip", required = false) String ipParam,
            HttpServletRequest request) {
        String ipToLookup;
        if (ipParam == null || ipParam.isBlank()) {
            ipToLookup = extractClientIp(request);
        } else if (isIpAddress(ipParam)) {
            ipToLookup = ipParam;
        } else {
            return ResponseEntity.badRequest().body("Invalid IP address");
        }

        Optional<LocationResponse> response = locationService.lookup(ipToLookup);
        return response.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Location not found"));
    }

    private boolean isIpAddress(String ip) {
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            log.debug("Invalid IP address provided: {}", ip);
            return false;
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
