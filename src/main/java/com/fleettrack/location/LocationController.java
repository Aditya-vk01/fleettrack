package com.fleettrack.location;

import java.util.List;

import com.fleettrack.location.dto.LocationAcceptedResponse;
import com.fleettrack.location.dto.LocationResponse;
import com.fleettrack.location.dto.LocationUpdateRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/couriers/{courierId}/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /** 202 Accepted: the ping is stored; downstream processing happens asynchronously. */
    @PostMapping
    public ResponseEntity<LocationAcceptedResponse> report(@PathVariable Long courierId,
                                                           @Valid @RequestBody LocationUpdateRequest request) {
        return ResponseEntity.accepted().body(locationService.ingest(courierId, request));
    }

    @GetMapping
    public List<LocationResponse> history(@PathVariable Long courierId,
                                          @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit) {
        return locationService.recentHistory(courierId, limit);
    }
}
