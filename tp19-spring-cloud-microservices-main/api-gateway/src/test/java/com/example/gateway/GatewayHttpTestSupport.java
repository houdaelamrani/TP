package com.example.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import static org.assertj.core.api.Assertions.assertThat;

abstract class GatewayHttpTestSupport {
    // Serveur HTTP ephemere : aucun microservice externe n'est requis.
    static final DisposableServer BACKEND = HttpServer.create().host("127.0.0.1").port(0)
            .handle((request, response) -> response.header("Content-Type", "text/plain")
                    .sendString(Mono.just(request.method().name() + " " + request.uri())))
            .bindNow();
    @LocalServerPort int port;

    @Test
    void patientRoutesPreservePaths() throws Exception {
        for (String path : new String[]{"/patients", "/patient/1", "/patients/1"}) {
            var response = request("GET", path);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("GET " + path);
        }
    }

    @Test
    void rdvRoutesPreservePathsAndWriteMethods() throws Exception {
        for (String path : new String[]{"/rdv", "/rdv/1", "/rdv/patient/1"}) {
            assertThat(request("GET", path).body()).isEqualTo("GET " + path);
        }
        assertThat(request("POST", "/rdv/1").body()).isEqualTo("POST /rdv/1");
        assertThat(request("PUT", "/rdv/1").body()).isEqualTo("PUT /rdv/1");
    }

    @Test
    void unmatchedPathReturns404() throws Exception {
        assertThat(request("GET", "/unknown").statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> request(String method, String path) throws Exception {
        try (HttpClient http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
