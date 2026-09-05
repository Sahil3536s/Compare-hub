package com.comparehub.controller;

import com.comparehub.dto.SavedRouteDto;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.RideSearch;
import com.comparehub.model.User;
import com.comparehub.repository.RideSearchRepository;
import com.comparehub.repository.UserRepository;
import com.comparehub.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/saved/routes")
@RequiredArgsConstructor
public class SavedRouteController {

    private final RideSearchRepository rideSearchRepository;
    private final UserRepository userRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<SavedRouteDto> saveRoute(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SavedRouteDto request) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RideSearch route = RideSearch.builder()
                .user(user)
                .pickupLocation(request.getPickup().trim())
                .dropLocation(request.getDestination().trim())
                .rideType(request.getRideType() != null ? request.getRideType().trim() : "all")
                .build();

        RideSearch saved = rideSearchRepository.save(route);
        return ResponseEntity.status(HttpStatus.CREATED).body(SavedRouteDto.builder()
                .id(saved.getId())
                .pickup(saved.getPickupLocation())
                .destination(saved.getDropLocation())
                .rideType(saved.getRideType())
                .createdAt(saved.getCreatedAt())
                .build());
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<SavedRouteDto>> getSavedRoutes(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<SavedRouteDto> routes = rideSearchRepository.findByUserIdOrderByCreatedAtDesc(userPrincipal.getId()).stream()
                .map(r -> SavedRouteDto.builder()
                        .id(r.getId())
                        .pickup(r.getPickupLocation())
                        .destination(r.getDropLocation())
                        .rideType(r.getRideType())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(routes);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteSavedRoute(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("id") Long id) {
        if (!rideSearchRepository.existsByIdAndUserId(id, userPrincipal.getId())) {
            throw new ResourceNotFoundException("Saved route not found with id: " + id);
        }
        rideSearchRepository.deleteByIdAndUserId(id, userPrincipal.getId());
        return ResponseEntity.ok(Map.of("message", "Route deleted from saved routes successfully"));
    }
}
