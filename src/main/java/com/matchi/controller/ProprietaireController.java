package com.matchi.controller;

import com.matchi.dto.LoginRequestDTO;
import com.matchi.dto.LoginResponseDTO;
import com.matchi.dto.ProprietaireDTO;
import com.matchi.dto.UpdatePasswordRequestDTO;
import com.matchi.service.ProprietaireService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proprietaires")
@RequiredArgsConstructor
@Tag(name = "Propriétaires")
public class ProprietaireController {

    private final ProprietaireService proprietaireService;

    // =======================
    // 🔹 Récupérer tous les propriétaires
    // =======================
    @GetMapping
    public List<ProprietaireDTO> getAllProprietaires() {
        return proprietaireService.findAll();
    }

    // =======================
    // 🔹 Récupérer un propriétaire par ID
    // =======================
    @GetMapping("/{id}")
    public ResponseEntity<ProprietaireDTO> getProprietaireById(@PathVariable Long id) {
        return ResponseEntity.ok(proprietaireService.findById(id));
    }

    // =======================
    // 🔹 Créer un propriétaire
    // =======================
    @PostMapping
    public ResponseEntity<ProprietaireDTO> createProprietaire(
            @RequestBody ProprietaireDTO dto
    ) {
        try {
            ProprietaireDTO created = proprietaireService.create(dto);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            throw e; // Le GlobalExceptionHandler va gérer l'exception
        }
    }

    // =======================
    // 🔹 Mettre à jour un propriétaire
    // =======================
    @PutMapping("/{id}")
    public ResponseEntity<ProprietaireDTO> updateProprietaire(
            @PathVariable Long id,
            @RequestBody ProprietaireDTO dto
    ) {
        ProprietaireDTO updated = proprietaireService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    // =======================
    // 🔹 Supprimer un propriétaire
    // =======================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProprietaire(@PathVariable Long id) {
        proprietaireService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // =======================
    // 🔹 Login propriétaire avec JWT
    // =======================
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = proprietaireService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    // =======================
    // 🔹 Mettre à jour le mot de passe d'un propriétaire
    // =======================
    @PutMapping("/{id}/mot-de-passe")
    public ResponseEntity<Void> updateMotPasseProprietaire(
            @PathVariable Long id,
            @RequestBody UpdatePasswordRequestDTO request
    ) {
        proprietaireService.updateMotPasseProprietaire(
                id,
                request.getMotDePasse(),
                request.getNewMotDePasse()
        );
        return ResponseEntity.ok().build();
    }
}
