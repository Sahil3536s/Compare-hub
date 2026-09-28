package com.comparehub.notification.channel;

import com.comparehub.model.Notification;
import com.comparehub.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationRepository notificationRepository;

    @Override
    public String getChannelName() {
        return "IN_APP";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void send(Notification notification) {
        if (notification.getId() == null) {
            notificationRepository.save(notification);
        }
        log.info("Dispatched IN_APP notification to user {}: [{}] {}",
                notification.getUser().getId(), notification.getType(), notification.getTitle());
    }
}
