package com.fleettrack.tracking.notification;

/** Port for SMS / push providers (Twilio, FCM, ...). */
public interface NotificationSender {

    void send(Notification notification);
}
