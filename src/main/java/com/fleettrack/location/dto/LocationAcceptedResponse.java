package com.fleettrack.location.dto;

import java.time.Instant;

/**
 * @param positionUpdated false when the ping was older than the courier's stored position
 *                        (stored in history, but did not move the courier)
 */
public record LocationAcceptedResponse(Long courierId, Instant recordedAt, boolean positionUpdated) {
}
