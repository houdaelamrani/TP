package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    Object exclusiveRoutingProfiles(Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("static"))
                && environment.acceptsProfiles(Profiles.of("discovery"))) {
            throw new IllegalStateException("Choisir un seul profil Gateway : static ou discovery");
        }
        return new Object();
    }
}
