package com.example.rdv;

import com.example.rdv.entities.RendezVous;
import com.example.rdv.repositories.RendezVousRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients
public class ServiceRdvApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceRdvApplication.class, args);
    }

    @Bean
    CommandLineRunner demoRendezVous(RendezVousRepository repository) {
        // Initialisation locale uniquement : aucun appel reseau au demarrage.
        return args -> repository.saveAll(List.of(
                new RendezVous("Dr Benjelloun", "Cardiologie", "Bilan cardiaque", 1L),
                new RendezVous("Dr Chraibi", "Dermatologie", "Consultation allergie", 1L),
                new RendezVous("Dr Fassi", "Médecine générale", "Certificat médical", 2L)));
    }
}
