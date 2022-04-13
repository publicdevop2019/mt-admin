package com.mt.access.port.adapter.persistence;

import com.mt.access.domain.model.project.ProjectId;
import com.mt.common.domain.model.domain_id.DomainId;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.AttributeConverter;

public class ProjectIdSetConverter implements AttributeConverter<Set<ProjectId>, String> {
    @Override
    public String convertToDatabaseColumn(Set<ProjectId> attribute) {
        if (attribute == null) {
            return null;
        }
        if (attribute.isEmpty()) {
            return null;
        }
        return String
            .join(",", attribute.stream().map(DomainId::getDomainId).collect(Collectors.toSet()));
    }

    @Override
    public Set<ProjectId> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (dbData.isBlank() || dbData.isEmpty()) {
            return null;
        }
        return Arrays.stream(dbData.split(",")).map(ProjectId::new).collect(Collectors.toSet());
    }
}
