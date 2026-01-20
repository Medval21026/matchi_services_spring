package com.matchi.controller;

import com.matchi.dto.ReservationPonctuelleDTO;
import com.matchi.service.ReservationPonctuelleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationPonctuelleController {

    private final ReservationPonctuelleService reservationService;

    @GetMapping
    public ResponseEntity<List<ReservationPonctuelleDTO>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationPonctuelleDTO> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @GetMapping("/terrain/{terrainId}")
    public ResponseEntity<List<ReservationPonctuelleDTO>> getReservationsByTerrain(@PathVariable Long terrainId) {
        return ResponseEntity.ok(reservationService.getReservationsByTerrain(terrainId));
    }

    @PostMapping
    public ResponseEntity<ReservationPonctuelleDTO> createReservation(@RequestBody ReservationPonctuelleDTO dto) {
        return ResponseEntity.ok(reservationService.createReservation(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationPonctuelleDTO> updateReservation(@PathVariable Long id,
                                                                     @RequestBody ReservationPonctuelleDTO dto) {
        return ResponseEntity.ok(reservationService.updateReservation(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
