package com.example.rdv.repositories;

import com.example.rdv.entities.RendezVous;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {
    List<RendezVous> findByPatientId(Long patientId);
}
