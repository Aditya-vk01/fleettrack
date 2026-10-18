package com.fleettrack.location;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fleettrack.common.exception.BadRequestException;
import com.fleettrack.common.exception.ResourceNotFoundException;
import com.fleettrack.courier.CourierRepository;
import com.fleettrack.events.LocationUpdatedEvent;
import com.fleettrack.location.dto.LocationAcceptedResponse;
import com.fleettrack.location.dto.LocationResponse;
import com.fleettrack.location.dto.LocationUpdateRequest;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The location write path. It does only what must be synchronous: store the ping and
 * move the courier's latest position. Everything downstream (ETA, notifications, metrics,
 * live map) happens asynchronously off Kafka, so a slow or failing consumer can never
 * slow down or fail location ingestion.
 */
@Service
public class LocationService {

    /** Devices with a slightly fast clock are tolerated; anything beyond this is rejected. */
    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(2);

    private final CourierRepository courierRepository;
    private final CourierLocationRepository locationRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LocationService(CourierRepository courierRepository,
                           CourierLocationRepository locationRepository,
                           ApplicationEventPublisher events,
                           Clock clock) {
        this.courierRepository = courierRepository;
        this.locationRepository = locationRepository;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public LocationAcceptedResponse ingest(Long courierId, LocationUpdateRequest request) {
        if (!courierRepository.existsById(courierId)) {
            throw new ResourceNotFoundException("Courier", courierId);
        }

        Instant receivedAt = clock.instant();
        Instant recordedAt = request.recordedAt() == null ? receivedAt : request.recordedAt();
        if (recordedAt.isAfter(receivedAt.plus(MAX_CLOCK_SKEW))) {
            throw new BadRequestException("recordedAt is in the future");
        }

        locationRepository.save(new CourierLocation(courierId, request.latitude(), request.longitude(),
                request.speedKmh(), request.headingDegrees(), recordedAt, receivedAt));

        boolean positionUpdated = courierRepository.updateLastPosition(
                courierId, request.latitude(), request.longitude(), recordedAt) == 1;

        if (positionUpdated) {
            // Spring application event, bridged to Kafka only AFTER this transaction commits
            // (see KafkaEventPublisher), so consumers never see a ping that was rolled back.
            events.publishEvent(new LocationUpdatedEvent(courierId, request.latitude(), request.longitude(),
                    request.speedKmh(), request.headingDegrees(), recordedAt, receivedAt));
        }
        return new LocationAcceptedResponse(courierId, recordedAt, positionUpdated);
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> recentHistory(Long courierId, int limit) {
        if (!courierRepository.existsById(courierId)) {
            throw new ResourceNotFoundException("Courier", courierId);
        }
        return locationRepository.findByCourierIdOrderByRecordedAtDesc(courierId, PageRequest.of(0, limit))
                .stream()
                .map(LocationResponse::from)
                .toList();
    }
}
