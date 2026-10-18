package com.fleettrack.tracking.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Default sender: logs instead of sending. Swap in a real provider by adding another implementation. */
@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(Notification notification) {
        log.info("[NOTIFY delivery={}] to {}: {}", notification.deliveryId(),
                mask(notification.recipientPhone()), notification.message());
    }

    private static String mask(String phone) {
        return phone == null || phone.length() < 4 ? "***" : "***" + phone.substring(phone.length() - 4);
    }
}
