package com.example.rdv.entities;

import com.example.rdv.dto.Patient;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity
public class RendezVous {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String medecin;
    private String specialite;
    private String motif;
    private Long patientId;
    @Transient
    private Patient patient;

    protected RendezVous() { }

    public RendezVous(String medecin, String specialite, String motif, Long patientId) {
        this.medecin = medecin;
        this.specialite = specialite;
        this.motif = motif;
        this.patientId = patientId;
    }

    public Long getId() { return id; }
    public String getMedecin() { return medecin; }
    public void setMedecin(String medecin) { this.medecin = medecin; }
    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
}
