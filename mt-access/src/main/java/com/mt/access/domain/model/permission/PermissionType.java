package com.mt.access.domain.model.permission;

import com.mt.common.domain.model.sql.converter.EnumConverter;

/**
 * Permission type
 */
public enum PermissionType {
    COMMON,
    PROJECT,
    API,
    API_ROOT;

    public static class DbConverter extends EnumConverter<PermissionType> {
        public DbConverter() {
            super(PermissionType.class);
        }
    }
}
