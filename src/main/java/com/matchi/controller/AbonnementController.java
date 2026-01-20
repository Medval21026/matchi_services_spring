package com.matchi.controller;

import com.matchi.dto.AbonnementDTO;
import com.matchi.dto.AbonnementCreateDTO;
import com.matchi.dto.AbonnementUpdateDTO;
import com.matchi.service.AbonnementService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/abonnements")
@RequiredArgsConstructor
public class AbonnementController {

    private final AbonnementService abonnementService;

    // ===== GET ALL =====
    @GetMapping
    public ResponseEntity<List<AbonnementDTO>> getAllAbonnements() {
        List<AbonnementDTO> list = abonnementService.getAllAbonnements();
        return ResponseEntity.ok(list);
    }

    // ===== GET BY ID =====
    @GetMapping("/{id}")
    public ResponseEntity<AbonnementDTO> getAbonnementById(@PathVariable Long id) {
        try {
            AbonnementDTO abonnement = abonnementService.getAbonnementById(id);
            return ResponseEntity.ok(abonnement);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ===== GET BY CLIENT =====
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<AbonnementDTO>> getAbonnementsByClientId(@PathVariable Long clientId) {
        List<AbonnementDTO> list = abonnementService.getAbonnementsByClientId(clientId);
        return ResponseEntity.ok(list);
    }

    // ===== CREATE =====
    @PostMapping
    public ResponseEntity<AbonnementDTO> createAbonnement(@RequestBody AbonnementCreateDTO dto) {
        AbonnementDTO created = abonnementService.createAbonnement(dto);
        return ResponseEntity.ok(created);
    }

    // ===== UPDATE =====
    @PutMapping("/{id}")
    public ResponseEntity<AbonnementDTO> updateAbonnement(@PathVariable Long id, @RequestBody AbonnementUpdateDTO dto) {
        try {
            AbonnementDTO updated = abonnementService.updateAbonnement(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ===== DELETE =====
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAbonnement(@PathVariable Long id) {
        try {
            abonnementService.deleteAbonnement(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
