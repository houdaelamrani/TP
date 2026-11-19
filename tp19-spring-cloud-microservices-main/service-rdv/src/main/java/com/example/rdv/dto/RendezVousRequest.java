package com.example.rdv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Le patientId du POST vient du chemin ; celui du PUT peut remplacer le patient. */
public record RendezVousRequest(@NotBlank String medecin, @NotBlank String specialite,
                             @NotBlank String motif, @Positive Long patientId) { }
