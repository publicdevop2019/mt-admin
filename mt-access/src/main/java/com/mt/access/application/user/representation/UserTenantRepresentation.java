package com.mt.access.application.user.representation;

import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.role.Role;
import com.mt.access.domain.model.role.RoleId;
import com.mt.access.domain.model.role.RoleQuery;
import com.mt.access.domain.model.user.User;
import com.mt.access.domain.model.user.UserRelation;
import com.mt.common.domain.model.restful.query.QueryUtility;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;

@Data
public class UserTenantRepresentation {
    private final String id;
    private final String displayName;
    private final Integer version;
    private Set<RoleDetail> roleDetails;

    public UserTenantRepresentation(UserRelation userRelation, User user) {
        this.id = user.getUserId().getDomainId();
        this.displayName = user.getDisplayName();
        this.version = userRelation.getVersion();
        if (userRelation.getStandaloneRoles() != null
            && userRelation.getStandaloneRoles().size() > 0) {
            Set<RoleId> standaloneRoles = userRelation.getStandaloneRoles();
            Set<Role> allByQuery = QueryUtility
                .getAllByQuery(e -> DomainRegistry.getRoleRepository().query(e),
                    new RoleQuery(standaloneRoles));
            roleDetails =
                allByQuery.stream().map(RoleDetail::new).collect(Collectors.toCollection(
                    LinkedHashSet::new));
        }


    }

    @Getter
    private static class RoleDetail {
        private final String id;
        private final String name;

        public RoleDetail(Role e) {
            id = e.getRoleId().getDomainId();
            name = e.getName();
        }
    }
}
