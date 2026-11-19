package com.example.rdv.patients;

import com.example.rdv.dto.Patient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "SERVICE-PATIENT")
public interface PatientFeignClient {
    @GetMapping("/patient/{id}")
    Patient findById(@PathVariable("id") Long id);
}
