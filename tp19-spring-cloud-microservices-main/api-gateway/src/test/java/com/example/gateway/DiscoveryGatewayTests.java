package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
@ActiveProfiles("discovery")
class DiscoveryGatewayTests extends GatewayHttpTestSupport {
    @Autowired GatewayProperties properties;

    @DynamicPropertySource
    static void discovery(DynamicPropertyRegistry registry) {
        // SimpleDiscoveryPatient remplace Eureka uniquement dans ce test isole.
        for (String service : new String[]{"SERVICE-PATIENT", "SERVICE-RDV"}) {
            registry.add("spring.cloud.discovery.client.simple.instances." + service + "[0].uri",
                    () -> "http://127.0.0.1:" + BACKEND.port());
        }
    }

    @Test
    void onlyLogicalServiceUrisAreLoaded() {
        assertThat(properties.getRoutes()).hasSize(2)
                .extracting(route -> route.getId()).containsExactly("patients-discovery", "rdv-discovery");
        assertThat(properties.getRoutes()).extracting(route -> route.getUri().toString())
                .containsExactly("lb://SERVICE-PATIENT", "lb://SERVICE-RDV");
    }
}
