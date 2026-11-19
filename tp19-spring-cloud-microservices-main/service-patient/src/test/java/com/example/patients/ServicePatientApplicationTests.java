package com.example.patients;

import com.example.patients.entities.Patient;
import com.example.patients.repositories.PatientRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class ServicePatientApplicationTests {
    @LocalServerPort int port;
    @Autowired PatientRepository repository;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void repositoryGeneratesIdentifiersAndPersistsFields() {
        Patient patient = repository.saveAndFlush(new Patient("Test", 30f));
        try {
            assertThat(patient.getId()).isPositive();
            assertThat(repository.findById(patient.getId())).get()
                    .extracting(Patient::getNom, Patient::getAge).containsExactly("Test", 30f);
        } finally {
            repository.deleteById(patient.getId());
        }
    }

    @Test
    void listContainsThreeDemoPatients() throws Exception {
        var response = get("/patients");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode patients = json.readTree(response.body());
        assertThat(patients.size()).isEqualTo(3);
        assertThat(patients.get(0).get("nom").asText()).isEqualTo("Salma BENNANI");
    }

    @Test
    void bothAliasesReturnSamePatient() throws Exception {
        var singular = get("/patient/1");
        var plural = get("/patients/1");
        assertThat(singular.statusCode()).isEqualTo(200);
        assertThat(plural.statusCode()).isEqualTo(200);
        assertThat(json.readTree(singular.body())).isEqualTo(json.readTree(plural.body()));
        assertThat(json.readTree(singular.body()).get("age").asDouble()).isEqualTo(34);
    }

    @Test
    void unknownPatientReturns404() throws Exception {
        assertThat(get("/patient/9999").statusCode()).isEqualTo(404);
        assertThat(get("/patients/9999").statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> get(String path) throws Exception {
        try (HttpClient http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
