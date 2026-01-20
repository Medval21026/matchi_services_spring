package com.matchi.controller;

import com.matchi.dto.AbonnementHoraireDTO;
import com.matchi.service.AbonnementHoraireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horaires")
@RequiredArgsConstructor
public class AbonnementHoraireController {

    private final AbonnementHoraireService horaireService;

    // ======== GET ALL ========
    @GetMapping
    public ResponseEntity<List<AbonnementHoraireDTO>> getAllHoraires() {
        return ResponseEntity.ok(horaireService.getAllHoraires());
    }

    // ======== GET BY ID ========
    @GetMapping("/{id}")
    public ResponseEntity<AbonnementHoraireDTO> getHoraireById(@PathVariable Long id) {
        AbonnementHoraireDTO dto = horaireService.trouverHoraireParId(id);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ======== GET BY ABONNEMENT ========
    @GetMapping("/abonnement/{abonnementId}")
    public ResponseEntity<List<AbonnementHoraireDTO>> getHorairesByAbonnement(@PathVariable Long abonnementId) {
        return ResponseEntity.ok(horaireService.getHorairesParAbonnement(abonnementId));
    }

    // ======== CREATE ========
    @PostMapping
    public ResponseEntity<AbonnementHoraireDTO> createHoraire(@RequestBody AbonnementHoraireDTO dto) {
        AbonnementHoraireDTO saved = horaireService.ajouterHoraire(dto);
        return ResponseEntity.ok(saved);
    }

    // ======== UPDATE ========
    @PutMapping("/{id}")
    public ResponseEntity<AbonnementHoraireDTO> updateHoraire(@PathVariable Long id,
                                                              @RequestBody AbonnementHoraireDTO dto) {
        AbonnementHoraireDTO updated = horaireService.mettreAJourHoraire(id, dto);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ======== DELETE ========
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoraire(@PathVariable Long id) {
        boolean deleted = horaireService.supprimerHoraire(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
