package com.example.patients;

import com.example.patients.entities.Patient;
import com.example.patients.repositories.PatientRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ServicePatientApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicePatientApplication.class, args);
    }

    @Bean
    CommandLineRunner demoPatients(PatientRepository repository) {
        return args -> repository.saveAll(List.of(
                new Patient("Salma BENNANI", 34f),
                new Patient("Youssef TAZI", 41f),
                new Patient("Khadija ALAOUI", 29f)));
    }
}
