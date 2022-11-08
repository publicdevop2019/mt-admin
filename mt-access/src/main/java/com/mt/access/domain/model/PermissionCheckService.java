package com.mt.access.domain.model;

import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.permission.Permission;
import com.mt.access.domain.model.permission.PermissionQuery;
import com.mt.access.domain.model.project.ProjectId;
import com.mt.access.domain.model.user.UserId;
import com.mt.access.infrastructure.AppConstant;
import com.mt.common.domain.model.audit.Auditable;
import com.mt.common.domain.model.restful.query.QueryUtility;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

@Service
public class PermissionCheckService {
    public void canAccess(@NotNull Set<ProjectId> ids, String permissionName) {
        if (ids == null) {
            throw new AccessDeniedException();
        }
        Set<ProjectId> collect = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (collect.size() == 0) {
            throw new AccessDeniedException();
        }
        //first check access to tenant project, query projectId must be one of jwt tenant ids
        Set<ProjectId> authorizedTenantId = DomainRegistry.getCurrentUserService().getTenantIds();
        boolean b = authorizedTenantId.containsAll(ids);
        if (!b) {
            throw new AccessDeniedException();
        }
        //second check if has read client access to current project
        PermissionQuery permissionQuery = PermissionQuery
            .ofProjectWithTenantIds(new ProjectId(AppConstant.MT_AUTH_PROJECT_ID), ids);
        permissionQuery.setNames(Collections.singleton(permissionName));
        Set<Permission> allByQuery = QueryUtility
            .getAllByQuery(e -> DomainRegistry.getPermissionRepository().getByQuery(e),
                permissionQuery);
        boolean b1 = DomainRegistry.getCurrentUserService().getPermissionIds().containsAll(
            allByQuery.stream().map(Permission::getPermissionId).collect(Collectors.toSet()));
        if (!b1) {
            throw new AccessDeniedException();
        }
    }

    public void canAccess(@NotNull ProjectId id, String permissionName) {
        canAccess(Collections.singleton(id), permissionName);
    }

    public void sameCreatedBy(Auditable e) {
        UserId userId = DomainRegistry.getCurrentUserService().getUserId();
        if (!new UserId(e.getCreatedBy()).equals(userId)) {
            throw new AccessDeniedException();
        }
    }
}
