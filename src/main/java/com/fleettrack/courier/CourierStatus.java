package com.fleettrack.courier;

public enum CourierStatus {
    /** Online and free to take a delivery. */
    AVAILABLE,
    /** Has an active delivery. Only the delivery lifecycle moves a courier in and out of BUSY. */
    BUSY,
    /** Not working. */
    OFFLINE
}
