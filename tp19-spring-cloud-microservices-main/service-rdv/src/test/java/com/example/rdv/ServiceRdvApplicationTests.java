package com.example.rdv;

import com.example.rdv.patients.PatientFeignClient;
import com.example.rdv.dto.Patient;
import com.example.rdv.entities.RendezVous;
import com.example.rdv.repositories.RendezVousRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class ServiceRdvApplicationTests {
    @LocalServerPort int port;
    @Autowired RendezVousRepository repository;
    @MockitoBean PatientFeignClient patients;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void mockPatients() {
        when(patients.findById(1L)).thenReturn(new Patient(1L, "Salma BENNANI", 34f));
        when(patients.findById(2L)).thenReturn(new Patient(2L, "Youssef TAZI", 41f));
        when(patients.findById(3L)).thenReturn(new Patient(3L, "Khadija ALAOUI", 29f));
    }

    @Test
    void repositoryFindsOwnerAndDoesNotPersistDto() {
        assertThat(repository.findByPatientId(1L)).hasSize(2);
        RendezVous rdv = new RendezVous("Test", "TEST", "Motif", 3L);
        rdv.setPatient(new Patient(3L, "Khadija ALAOUI", 29f));
        rdv = repository.saveAndFlush(rdv);
        try {
            assertThat(rdv.getId()).isPositive();
            assertThat(repository.findById(rdv.getId()).orElseThrow().getPatient()).isNull();
        } finally {
            repository.deleteById(rdv.getId());
        }
    }

    @Test
    void listIsEnrichedWithOneFeignCallPerOwner() throws Exception {
        var response = request("GET", "/rdv", null);
        assertThat(response.statusCode()).isEqualTo(200);
        var body = json.readTree(response.body());
        assertThat(body.size()).isEqualTo(3);
        assertThat(body.get(0).get("patient").get("nom").asText()).isEqualTo("Salma BENNANI");
        verify(patients, times(1)).findById(1L);
        verify(patients, times(1)).findById(2L);
    }

    @Test
    void getByIdAndOwnerAreEnriched() throws Exception {
        var one = request("GET", "/rdv/1", null);
        assertThat(one.statusCode()).isEqualTo(200);
        assertThat(json.readTree(one.body()).get("patient").get("id").asLong()).isEqualTo(1);
        var owner = request("GET", "/rdv/patient/1", null);
        assertThat(owner.statusCode()).isEqualTo(200);
        assertThat(json.readTree(owner.body()).size()).isEqualTo(2);
        var empty = request("GET", "/rdv/patient/3", null);
        assertThat(empty.statusCode()).isEqualTo(200);
        assertThat(json.readTree(empty.body()).isEmpty()).isTrue();
    }

    @Test
    void createThenUpdateAndRead() throws Exception {
        var created = request("POST", "/rdv/1", """
                {"medecin":"Dr Test","specialite":"TEST","motif":"Controle"}
                """);
        assertThat(created.statusCode()).isEqualTo(201);
        Long id = json.readTree(created.body()).get("id").asLong();
        try {
            assertThat(created.headers().firstValue("Location")).contains("/rdv/" + id);
            var updated = request("PUT", "/rdv/" + id, """
                    {"medecin":"Dr Test","specialite":"NEW","motif":"Suivi","patientId":2}
                    """);
            assertThat(updated.statusCode()).isEqualTo(200);
            var read = request("GET", "/rdv/" + id, null);
            var body = json.readTree(read.body());
            assertThat(body.get("motif").asText()).isEqualTo("Suivi");
            assertThat(body.get("patient").get("nom").asText()).isEqualTo("Youssef TAZI");
        } finally {
            repository.deleteById(id);
        }
    }

    @Test
    void missingRendezVousReturns404WithoutFeignCall() throws Exception {
        assertThat(request("GET", "/rdv/9999", null).statusCode()).isEqualTo(404);
        assertThat(request("PUT", "/rdv/9999", validBody()).statusCode()).isEqualTo(404);
        verifyNoInteractions(patients);
    }

    @Test
    void missingRemotePatientReturns404AndDoesNotCreate() throws Exception {
        when(patients.findById(9999L)).thenThrow(feignError(404));
        long count = repository.count();
        assertThat(request("POST", "/rdv/9999", validBody()).statusCode()).isEqualTo(404);
        assertThat(request("GET", "/rdv/patient/9999", null).statusCode()).isEqualTo(404);
        assertThat(repository.count()).isEqualTo(count);
    }

    @Test
    void remoteFailureReturns503ForReadsAndWrites() throws Exception {
        when(patients.findById(1L)).thenThrow(feignError(503));
        long count = repository.count();
        assertThat(request("GET", "/rdv", null).statusCode()).isEqualTo(503);
        assertThat(request("GET", "/rdv/1", null).statusCode()).isEqualTo(503);
        assertThat(request("POST", "/rdv/1", validBody()).statusCode()).isEqualTo(503);
        assertThat(repository.count()).isEqualTo(count);
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        assertThat(request("POST", "/rdv/1", "{}").statusCode()).isEqualTo(400);
        verifyNoInteractions(patients);
    }

    private String validBody() {
        return "{\"medecin\":\"Test\",\"specialite\":\"TEST\",\"motif\":\"Test\"}";
    }

    private FeignException feignError(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://SERVICE-PATIENT/patient/1", Map.of(), (byte[]) null, null, null);
        return FeignException.errorStatus("findById", Response.builder().status(status).reason("Test").request(request).headers(Map.of()).build());
    }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) builder.header("Content-Type", "application/json");
        try (HttpClient http = HttpClient.newHttpClient()) {
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
