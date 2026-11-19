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
@ActiveProfiles("static")
class StaticGatewayTests extends GatewayHttpTestSupport {
    @Autowired GatewayProperties properties;

    @DynamicPropertySource
    static void backend(DynamicPropertyRegistry registry) {
        registry.add("gateway.patients-uri", () -> "http://127.0.0.1:" + BACKEND.port());
        registry.add("gateway.rdv-uri", () -> "http://127.0.0.1:" + BACKEND.port());
    }

    @Test
    void onlyTwoStaticHttpRoutesAreLoaded() {
        assertThat(properties.getRoutes()).hasSize(2)
                .extracting(route -> route.getId()).containsExactly("patients-static", "rdv-static");
        assertThat(properties.getRoutes()).allSatisfy(route ->
                assertThat(route.getUri().getScheme()).isEqualTo("http"));
    }
}
