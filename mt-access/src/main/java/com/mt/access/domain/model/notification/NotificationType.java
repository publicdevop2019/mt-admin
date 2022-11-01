package com.mt.access.domain.model.notification;

import com.mt.common.domain.model.sql.converter.EnumConverter;

public enum NotificationType {
    EMAIL,
    BELL,
    SMS;

    public static class DbConverter extends EnumConverter<NotificationType> {
        public DbConverter() {
            super(NotificationType.class);
        }
    }
}