package com.fleettrack.delivery;

import java.net.URI;
import java.util.List;

import com.fleettrack.common.exception.ResourceNotFoundException;
import com.fleettrack.delivery.dto.AssignDeliveryRequest;
import com.fleettrack.delivery.dto.CreateDeliveryRequest;
import com.fleettrack.delivery.dto.DeliveryResponse;
import com.fleettrack.delivery.dto.StatusChangeResponse;
import com.fleettrack.delivery.dto.TransitionRequest;
import com.fleettrack.tracking.eta.EtaEstimate;
import com.fleettrack.tracking.eta.EtaStore;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final EtaStore etaStore;

    public DeliveryController(DeliveryService deliveryService, EtaStore etaStore) {
        this.deliveryService = deliveryService;
        this.etaStore = etaStore;
    }

    @PostMapping
    public ResponseEntity<DeliveryResponse> create(@Valid @RequestBody CreateDeliveryRequest request) {
        DeliveryResponse created = deliveryService.create(request);
        return ResponseEntity.created(URI.create("/api/deliveries/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public DeliveryResponse get(@PathVariable Long id) {
        return deliveryService.get(id);
    }

    @GetMapping
    public List<DeliveryResponse> list(@RequestParam(required = false) DeliveryStatus status) {
        return deliveryService.list(status);
    }

    /** Body is optional: {} or no body = auto-assign nearest available courier. */
    @PostMapping("/{id}/assign")
    public DeliveryResponse assign(@PathVariable Long id,
                                   @Valid @RequestBody(required = false) AssignDeliveryRequest request) {
        return deliveryService.assign(id, request);
    }

    /** Example body: {"status": "PICKED_UP"} */
    @PostMapping("/{id}/status")
    public DeliveryResponse transition(@PathVariable Long id, @Valid @RequestBody TransitionRequest request) {
        return deliveryService.transition(id, request.status());
    }

    @GetMapping("/{id}/history")
    public List<StatusChangeResponse> history(@PathVariable Long id) {
        return deliveryService.history(id);
    }

    /** Latest ETA computed by the ETA consumer (cached in Redis). */
    @GetMapping("/{id}/eta")
    public EtaEstimate eta(@PathVariable Long id) {
        return etaStore.find(id).orElseThrow(() -> new ResourceNotFoundException("ETA for delivery", id));
    }
}
