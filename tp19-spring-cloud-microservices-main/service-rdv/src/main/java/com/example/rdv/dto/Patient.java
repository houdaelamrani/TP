package com.example.rdv.dto;

/** Copie de transport du patient distant, sans mapping JPA. */
public record Patient(Long id, String nom, Float age) { }
