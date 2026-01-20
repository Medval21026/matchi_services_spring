package com.matchi.service;

import com.matchi.dto.ClientAbonneDTO;
import com.matchi.model.ClientAbonne;
import com.matchi.repository.ClientAbonneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientAbonneService {

    private final ClientAbonneRepository repository;

    // =====================
    // CREATE
    // =====================
    public ClientAbonneDTO create(ClientAbonneDTO dto) {

        if (repository.existsByTelephone(dto.telephone())) {
            throw new RuntimeException("Numéro de téléphone déjà utilisé");
        }

        ClientAbonne client = new ClientAbonne();
        client.setNom(dto.nom());
        client.setPrenom(dto.prenom());
        client.setTelephone(dto.telephone());

        return mapToDTO(repository.save(client));
    }

    // =====================
    // UPDATE
    // =====================
    public ClientAbonneDTO update(Long id, ClientAbonneDTO dto) {

        ClientAbonne existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));

        // Mise à jour partielle - uniquement les champs non-null
        if (dto.nom() != null) {
            existing.setNom(dto.nom());
        }
        if (dto.prenom() != null) {
            existing.setPrenom(dto.prenom());
        }
        if (dto.telephone() != null) {
            // ✅ VALIDATION : Vérifier si le nouveau numéro de téléphone existe déjà pour un autre client
            if (!dto.telephone().equals(existing.getTelephone())) {
                // Le numéro de téléphone est différent, vérifier s'il existe déjà
                Optional<ClientAbonne> clientAvecMemeTelephone = repository.findByTelephone(dto.telephone());
                if (clientAvecMemeTelephone.isPresent() && !clientAvecMemeTelephone.get().getId().equals(id)) {
                    throw new RuntimeException(
                        String.format("Le numéro de téléphone %d est déjà utilisé par un autre client", dto.telephone())
                    );
                }
            }
            existing.setTelephone(dto.telephone());
        }

        return mapToDTO(repository.save(existing));
    }

    // =====================
    // DELETE
    // =====================
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Client introuvable");
        }
        repository.deleteById(id);
    }

    // =====================
    // FIND BY ID
    // =====================
    public ClientAbonneDTO findById(Long id) {
        return mapToDTO(
                repository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Client introuvable"))
        );
    }

    // =====================
    // FIND ALL
    // =====================
    public List<ClientAbonneDTO> findAll() {
        return repository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // =====================
    // FIND BY TELEPHONE
    // =====================
    public ClientAbonneDTO findByTelephone(Integer telephone) {
        return mapToDTO(
                repository.findByTelephone(telephone)
                        .orElseThrow(() -> new RuntimeException("Client introuvable"))
        );
    }

    // =====================
    // MAPPER (Entity → DTO)
    // =====================
    private ClientAbonneDTO mapToDTO(ClientAbonne client) {
        return new ClientAbonneDTO(
                client.getId(),
                client.getNom(),
                client.getPrenom(),
                client.getTelephone()
        );
    }
}
