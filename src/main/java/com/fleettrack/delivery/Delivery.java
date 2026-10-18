package com.fleettrack.delivery;

import java.time.Instant;

import com.fleettrack.common.exception.InvalidStateTransitionException;
import com.fleettrack.courier.Courier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A delivery job. Status only changes through {@link #assignTo} and {@link #transitionTo},
 * which enforce {@link DeliveryStatus}'s transition rules - there is no public status setter.
 */
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "pickup_latitude", nullable = false)
    private double pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false)
    private double pickupLongitude;

    @Column(name = "dropoff_address")
    private String dropoffAddress;

    @Column(name = "dropoff_latitude", nullable = false)
    private double dropoffLatitude;

    @Column(name = "dropoff_longitude", nullable = false)
    private double dropoffLongitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private Courier courier;

    /** Two dispatchers changing the same delivery at once: the second one gets a 409, not a lost update. */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Delivery() {
        // for JPA
    }

    public Delivery(String recipientName, String recipientPhone,
                    String pickupAddress, double pickupLatitude, double pickupLongitude,
                    String dropoffAddress, double dropoffLatitude, double dropoffLongitude) {
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.pickupAddress = pickupAddress;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropoffAddress = dropoffAddress;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.status = DeliveryStatus.CREATED;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** CREATED -> ASSIGNED, attaching the courier. */
    public DeliveryStatus assignTo(Courier courier, Instant at) {
        DeliveryStatus previous = transitionTo(DeliveryStatus.ASSIGNED, at);
        this.courier = courier;
        return previous;
    }

    /**
     * The only way to change status. Rejects anything the state machine does not allow.
     *
     * @return the status before the transition
     */
    public DeliveryStatus transitionTo(DeliveryStatus target, Instant at) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(status, target);
        }
        DeliveryStatus previous = status;
        status = target;
        switch (target) {
            case ASSIGNED -> assignedAt = at;
            case PICKED_UP -> pickedUpAt = at;
            case DELIVERED -> deliveredAt = at;
            case CANCELLED -> cancelledAt = at;
            default -> {
                // IN_TRANSIT has no dedicated timestamp; the audit table records it.
            }
        }
        return previous;
    }

    public Long getId() {
        return id;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public double getDropoffLatitude() {
        return dropoffLatitude;
    }

    public double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public Courier getCourier() {
        return courier;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }
}
