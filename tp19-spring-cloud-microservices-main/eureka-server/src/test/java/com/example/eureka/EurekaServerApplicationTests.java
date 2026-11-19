package com.example.eureka;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerApplicationTests {
    @LocalServerPort
    int port;

    @Test
    void registryAndDashboardAreAvailable() throws Exception {
        try (HttpClient http = HttpClient.newHttpClient()) {
            var dashboard = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(dashboard.statusCode()).isEqualTo(200);
            assertThat(dashboard.body()).contains("Eureka");
            var registry = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/eureka/apps")).header("Accept", "application/json").build(), HttpResponse.BodyHandlers.ofString());
            assertThat(registry.statusCode()).isEqualTo(200);
            assertThat(registry.body()).contains("applications");
        }
    }
}
