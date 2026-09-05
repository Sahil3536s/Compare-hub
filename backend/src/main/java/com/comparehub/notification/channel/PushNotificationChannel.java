package com.comparehub.notification.channel;

import com.comparehub.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PushNotificationChannel implements NotificationChannel {

    @Value("${app.notifications.push.enabled:false}")
    private boolean pushEnabled;

    @Override
    public String getChannelName() {
        return "PUSH";
    }

    @Override
    public boolean isEnabled() {
        return pushEnabled;
    }

    @Override
    public void send(Notification notification) {
        if (!isEnabled()) {
            return;
        }
        log.info("Mock PUSH notification dispatched to user {}: [{}] {}",
                notification.getUser().getId(), notification.getType(), notification.getTitle());
    }
}
