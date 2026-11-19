package com.example.rdv.services;

import com.example.rdv.patients.PatientFeignClient;
import com.example.rdv.dto.Patient;
import com.example.rdv.dto.RendezVousRequest;
import com.example.rdv.entities.RendezVous;
import com.example.rdv.repositories.RendezVousRepository;
import feign.FeignException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RendezVousService {
    private final RendezVousRepository repository;
    private final PatientFeignClient patients;

    public RendezVousService(RendezVousRepository repository, PatientFeignClient patients) {
        this.repository = repository;
        this.patients = patients;
    }

    public List<RendezVous> findAll() { return enrich(repository.findAll()); }

    public RendezVous findById(Long id) {
        RendezVous rdv = requireRendezVous(id);
        rdv.setPatient(requirePatient(rdv.getPatientId()));
        return rdv;
    }

    public List<RendezVous> findByPatientId(Long patientId) {
        Patient patient = requirePatient(patientId);
        List<RendezVous> rdvs = repository.findByPatientId(patientId);
        rdvs.forEach(rdv -> rdv.setPatient(patient));
        return rdvs;
    }

    @Transactional
    public RendezVous create(Long patientId, RendezVousRequest request) {
        requirePatient(patientId);
        return repository.save(new RendezVous(request.medecin(), request.specialite(), request.motif(), patientId));
    }

    @Transactional
    public RendezVous update(Long id, RendezVousRequest request) {
        RendezVous rdv = requireRendezVous(id);
        Long patientId = request.patientId() == null ? rdv.getPatientId() : request.patientId();
        requirePatient(patientId);
        rdv.setMedecin(request.medecin());
        rdv.setSpecialite(request.specialite());
        rdv.setMotif(request.motif());
        rdv.setPatientId(patientId);
        return repository.save(rdv);
    }

    private RendezVous requireRendezVous(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable : " + id));
    }

    private Patient requirePatient(Long id) {
        try {
            return patients.findById(id);
        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable : " + id, exception);
        } catch (FeignException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE-PATIENT indisponible", exception);
        }
    }

    private List<RendezVous> enrich(List<RendezVous> rdvs) {
        // Cache limite a cette requete : un appel par patient distinct.
        Map<Long, Patient> cache = new HashMap<>();
        rdvs.forEach(rdv -> rdv.setPatient(cache.computeIfAbsent(rdv.getPatientId(), this::requirePatient)));
        return rdvs;
    }
}
