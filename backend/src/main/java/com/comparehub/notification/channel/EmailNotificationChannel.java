package com.comparehub.notification.channel;

import com.comparehub.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationChannel implements NotificationChannel {

    @Value("${app.notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public boolean isEnabled() {
        return emailEnabled;
    }

    @Override
    public void send(Notification notification) {
        if (!isEnabled()) {
            return;
        }
        log.info("Mock EMAIL dispatched to {}: [{}] {}",
                notification.getUser().getEmail(), notification.getType(), notification.getTitle());
    }
}
