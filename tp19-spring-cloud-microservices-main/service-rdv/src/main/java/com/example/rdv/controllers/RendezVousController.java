package com.example.rdv.controllers;

import com.example.rdv.dto.RendezVousRequest;
import com.example.rdv.entities.RendezVous;
import com.example.rdv.services.RendezVousService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rdv")
public class RendezVousController {
    private final RendezVousService service;

    public RendezVousController(RendezVousService service) { this.service = service; }

    @GetMapping
    public List<RendezVous> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public RendezVous findById(@PathVariable Long id) { return service.findById(id); }

    @GetMapping("/patient/{id}")
    public List<RendezVous> findByPatientId(@PathVariable Long id) { return service.findByPatientId(id); }

    @PostMapping("/{patientId}")
    public ResponseEntity<RendezVous> create(@PathVariable Long patientId, @Valid @RequestBody RendezVousRequest request) {
        RendezVous rdv = service.create(patientId, request);
        return ResponseEntity.created(URI.create("/rdv/" + rdv.getId())).body(rdv);
    }

    @PutMapping("/{id}")
    public RendezVous update(@PathVariable Long id, @Valid @RequestBody RendezVousRequest request) {
        return service.update(id, request);
    }
}
