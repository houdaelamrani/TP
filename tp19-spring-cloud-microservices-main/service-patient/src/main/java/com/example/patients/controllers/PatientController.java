package com.example.patients.controllers;

import com.example.patients.entities.Patient;
import com.example.patients.services.PatientService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PatientController {
    private final PatientService service;

    public PatientController(PatientService service) { this.service = service; }

    @GetMapping("/patients")
    public List<Patient> findAll() { return service.findAll(); }

    @GetMapping({"/patient/{id}", "/patients/{id}"})
    public Patient findById(@PathVariable Long id) { return service.findById(id); }
}
