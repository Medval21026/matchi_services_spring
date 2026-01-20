package com.matchi.controller;

import com.matchi.dto.ClientAbonneDTO;
import com.matchi.service.ClientAbonneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientAbonneController {

    private final ClientAbonneService service;

    // 🔹 1. Créer un client abonné
    @PostMapping
    public ClientAbonneDTO create(@RequestBody ClientAbonneDTO dto) {
        return service.create(dto);
    }

    // 🔹 2. Modifier un client
    @PutMapping("/{id}")
    public ClientAbonneDTO update(
            @PathVariable Long id,
            @RequestBody ClientAbonneDTO dto
    ) {
        return service.update(id, dto);
    }

    // 🔹 3. Supprimer un client
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // 🔹 4. Trouver un client par ID
    @GetMapping("/{id}")
    public ClientAbonneDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    // 🔹 5. Lister tous les clients
    @GetMapping
    public List<ClientAbonneDTO> findAll() {
        return service.findAll();
    }

    // 🔹 6. Trouver un client par téléphone
    @GetMapping("/telephone/{telephone}")
    public ClientAbonneDTO findByTelephone(@PathVariable Integer telephone) {
        return service.findByTelephone(telephone);
    }
}
