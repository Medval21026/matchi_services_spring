package com.matchi.dto;


public record ProprietaireDTO(
        Long id,
        String nom,
        String prenom,
        Integer telephone,
        String password,
        Boolean isActive
) {}
