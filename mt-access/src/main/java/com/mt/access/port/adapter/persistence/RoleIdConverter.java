package com.mt.access.port.adapter.persistence;

import com.mt.access.domain.model.role.RoleId;
import com.mt.common.domain.model.domain_event.DomainId;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.AttributeConverter;

public class RoleIdConverter implements AttributeConverter<RoleId, String> {
    @Override
    public String convertToDatabaseColumn(RoleId roleId) {
        if (roleId == null) {
            return null;
        }
        return roleId.getDomainId();
    }

    @Override
    public RoleId convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (dbData.isBlank() || dbData.isEmpty()) {
            return null;
        }
        return new RoleId(dbData);
    }
}