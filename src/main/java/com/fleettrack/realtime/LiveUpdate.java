package com.fleettrack.realtime;

/**
 * Envelope carried over Redis pub/sub.
 *
 * @param destination STOMP destination, e.g. /topic/couriers/42
 * @param payload     JSON body to deliver to subscribers
 */
public record LiveUpdate(String destination, String payload) {
}
