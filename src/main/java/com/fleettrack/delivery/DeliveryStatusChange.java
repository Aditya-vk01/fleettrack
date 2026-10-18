package com.fleettrack.delivery;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Audit row written for every delivery state change. */
@Entity
@Table(name = "delivery_status_changes")
public class DeliveryStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_id", nullable = false)
    private Long deliveryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private DeliveryStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private DeliveryStatus toStatus;

    @Column(name = "courier_id")
    private Long courierId;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected DeliveryStatusChange() {
        // for JPA
    }

    public DeliveryStatusChange(Long deliveryId, DeliveryStatus fromStatus, DeliveryStatus toStatus,
                                Long courierId, Instant changedAt) {
        this.deliveryId = deliveryId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.courierId = courierId;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public DeliveryStatus getFromStatus() {
        return fromStatus;
    }

    public DeliveryStatus getToStatus() {
        return toStatus;
    }

    public Long getCourierId() {
        return courierId;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
