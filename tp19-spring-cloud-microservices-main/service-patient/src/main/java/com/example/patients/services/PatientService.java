package com.example.patients.services;

import com.example.patients.entities.Patient;
import com.example.patients.repositories.PatientRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PatientService {
    private final PatientRepository repository;

    public PatientService(PatientRepository repository) { this.repository = repository; }

    public List<Patient> findAll() { return repository.findAll(); }

    public Patient findById(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable : " + id));
    }
}
