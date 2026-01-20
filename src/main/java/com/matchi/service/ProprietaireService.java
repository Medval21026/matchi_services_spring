package com.matchi.service;

import com.matchi.dto.LoginRequestDTO;
import com.matchi.dto.LoginResponseDTO;
import com.matchi.dto.ProprietaireDTO;
import com.matchi.model.Proprietaire;
import com.matchi.model.TerrainService;
import com.matchi.repository.ProprietaireRepository;
import com.matchi.repository.TerrainServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProprietaireService {

    private final ProprietaireRepository proprietaireRepository;
    private final TerrainServiceRepository terrainServiceRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // ================== CREATE ==================
    public ProprietaireDTO create(ProprietaireDTO dto) {

        if (proprietaireRepository.existsByTelephone(dto.telephone())) {
            throw new RuntimeException("Numéro de téléphone déjà utilisé");
        }

        Proprietaire proprietaire = mapToEntity(dto);

        // *** HACHAGE DU MOT DE PASSE ***
        if (dto.password() != null && !dto.password().isBlank()) {
            String hashedPassword = passwordEncoder.encode(dto.password());
            proprietaire.setPassword(hashedPassword);
        }

        // valeur par défaut : compte actif par défaut
        if (proprietaire.getIsActive() == null) {
            proprietaire.setIsActive(true);
        }

        Proprietaire saved = proprietaireRepository.save(proprietaire);
        return mapToDTO(saved);
    }

    // ================== UPDATE ==================
    public ProprietaireDTO update(Long id, ProprietaireDTO dto) {

        Proprietaire existing = proprietaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propriétaire introuvable"));

        // Mise à jour partielle
        if (dto.nom() != null) {
            existing.setNom(dto.nom());
        }
        if (dto.prenom() != null) {
            existing.setPrenom(dto.prenom());
        }
        if (dto.telephone() != null) {
            existing.setTelephone(dto.telephone());
        }
        
        // *** HACHAGE DU MOT DE PASSE SI MODIFIÉ ***
        if (dto.password() != null && !dto.password().isBlank()) {
            String hashedPassword = passwordEncoder.encode(dto.password());
            existing.setPassword(hashedPassword);
        }
        
        if (dto.isActive() != null) {
            existing.setIsActive(dto.isActive());
        }

        Proprietaire updated = proprietaireRepository.save(existing);
        return mapToDTO(updated);
    }

    // ================== DELETE ==================
    public void delete(Long id) {
        if (!proprietaireRepository.existsById(id)) {
            throw new RuntimeException("Propriétaire introuvable");
        }
        proprietaireRepository.deleteById(id);
    }

    // ================== FIND ==================
    public ProprietaireDTO findById(Long id) {
        return proprietaireRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Propriétaire introuvable"));
    }

    public List<ProprietaireDTO> findAll() {
        return proprietaireRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ================== MAPPERS ==================
    private ProprietaireDTO mapToDTO(Proprietaire p) {
        return new ProprietaireDTO(
                p.getId(),
                p.getNom(),
                p.getPrenom(),
                p.getTelephone(),
                null, // Ne jamais retourner le mot de passe (même haché) par sécurité
                p.getIsActive()
        );
    }

    private Proprietaire mapToEntity(ProprietaireDTO dto) {
        Proprietaire p = new Proprietaire();
        p.setNom(dto.nom());
        p.setPrenom(dto.prenom());
        p.setTelephone(dto.telephone());
        // Le mot de passe sera haché dans create() et update()
        p.setPassword(dto.password());
        p.setIsActive(dto.isActive());
        return p;
    }

    // ================== LOGIN ==================
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        // Vérifier que les champs ne sont pas null
        if (loginRequest.getTelephone() == null || loginRequest.getPassword() == null) {
            throw new RuntimeException("Le numéro de téléphone et le mot de passe sont obligatoires");
        }

        // Chercher le propriétaire par téléphone (peu importe s'il est actif ou non)
        Proprietaire proprietaire = proprietaireRepository.findAll().stream()
            .filter(p -> loginRequest.getTelephone().equals(p.getTelephone()))
            .findFirst()
            .orElse(null);

        // Si le propriétaire n'existe pas
        if (proprietaire == null) {
            throw new RuntimeException("Aucun compte trouvé avec ce numéro de téléphone");
        }

        // Si le compte n'est pas actif
        if (!Boolean.TRUE.equals(proprietaire.getIsActive())) {
            throw new RuntimeException("Ce compte n'est pas activé. Veuillez contacter l'administrateur");
        }

        // *** VERIFICATION DU MOT DE PASSE HACHE ***
        if (!passwordEncoder.matches(loginRequest.getPassword(), proprietaire.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        // *** GENERATION DU TOKEN JWT ***
        String token = jwtService.generateToken(
            proprietaire.getId(),
            proprietaire.getTelephone().toString(),
            proprietaire.getNom(),
            proprietaire.getPrenom()
        );

        // ✅ Récupérer les IDs des terrains associés au propriétaire
        List<Long> terrainIds = terrainServiceRepository.findByProprietaireId(proprietaire.getId())
                .stream()
                .map(TerrainService::getId)
                .collect(Collectors.toList());

        // Retourner la réponse avec le token et les IDs de terrains
        return new LoginResponseDTO(
            proprietaire.getId(),
            proprietaire.getNom(),
            proprietaire.getPrenom(),
            proprietaire.getTelephone(),
            proprietaire.getIsActive(),
            token,
            terrainIds
        );
    }
}