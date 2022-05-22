package com.mt.access.application.notification.representation;

import com.mt.access.domain.model.notification.Notification;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class NotificationRepresentation {
    private Long date;
    private String id;
    private String title;
    private Set<String> descriptions;

    public NotificationRepresentation(Notification notification) {
        date = notification.getTimestamp();
        id = notification.getNotificationId().getDomainId();
        descriptions = notification.getDescriptions();
        title = notification.getTitle();
    }
}
