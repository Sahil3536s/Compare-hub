package com.comparehub.notification.channel;

import com.comparehub.model.Notification;

public interface NotificationChannel {

    String getChannelName();

    boolean isEnabled();

    void send(Notification notification);
}
