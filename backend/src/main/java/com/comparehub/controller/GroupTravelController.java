package com.comparehub.controller;

import com.comparehub.dto.GroupTravelRequestDto;
import com.comparehub.dto.GroupTravelResponseDto;
import com.comparehub.service.GroupTravelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/travel/group-optimize")
@RequiredArgsConstructor
public class GroupTravelController {

    private final GroupTravelService groupTravelService;

    @PostMapping
    public ResponseEntity<GroupTravelResponseDto> optimizeGroupTravel(
            @Valid @RequestBody GroupTravelRequestDto request) {
        log.info("REST request to optimize group travel for {} travelers: {} -> {}",
                request.getNumberOfTravelers(), request.getOrigin(), request.getDestination());
        GroupTravelResponseDto response = groupTravelService.optimizeGroupTravel(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<GroupTravelResponseDto> getGroupTravelOptimization(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String travelDate,
            @RequestParam(defaultValue = "1") int numberOfTravelers,
            @RequestParam(required = false) Double budget,
            @RequestParam(defaultValue = "BALANCED") String priority) {

        GroupTravelRequestDto request = GroupTravelRequestDto.builder()
                .origin(origin)
                .destination(destination)
                .travelDate(travelDate)
                .numberOfTravelers(numberOfTravelers)
                .budget(budget)
                .priority(priority)
                .build();

        GroupTravelResponseDto response = groupTravelService.optimizeGroupTravel(request);
        return ResponseEntity.ok(response);
    }
}
