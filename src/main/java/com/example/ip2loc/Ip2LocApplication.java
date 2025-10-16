package com.example.ip2loc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the IP geolocation service.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class Ip2LocApplication {

    public static void main(String[] args) {
        SpringApplication.run(Ip2LocApplication.class, args);
    }
}
