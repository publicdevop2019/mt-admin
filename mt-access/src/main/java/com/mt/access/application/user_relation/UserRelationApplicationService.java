package com.mt.access.application.user_relation;

import com.mt.access.application.ApplicationServiceRegistry;
import com.mt.access.application.user.representation.UserTenantRepresentation;
import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.project.ProjectId;
import com.mt.access.domain.model.role.Role;
import com.mt.access.domain.model.role.RoleId;
import com.mt.access.domain.model.role.RoleQuery;
import com.mt.access.domain.model.role.event.NewProjectRoleCreated;
import com.mt.access.domain.model.user.User;
import com.mt.access.domain.model.user.UserId;
import com.mt.access.domain.model.user.UserQuery;
import com.mt.access.domain.model.user_relation.UserRelation;
import com.mt.access.domain.model.user_relation.UserRelationQuery;
import com.mt.access.infrastructure.AppConstant;
import com.mt.common.domain.model.domain_event.SubscribeForEvent;
import com.mt.common.domain.model.restful.SumPagedRep;
import com.mt.common.domain.model.restful.query.QueryUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mt.access.domain.model.permission.Permission.*;
import static com.mt.access.domain.model.role.Role.PROJECT_USER;

@Service
@Slf4j
public class UserRelationApplicationService {
    private static final String USER_RELATION = "UserRelation";

    public Optional<UserRelation> getUserRelation(UserId userId, ProjectId projectId) {
        return DomainRegistry.getUserRelationRepository().getByUserIdAndProjectId(userId, projectId);
    }

    /**
     * create user relation to mt-auth and target project as well
     *
     * @param event
     */
    @SubscribeForEvent
    @Transactional
    public void handle(NewProjectRoleCreated event) {
        ApplicationServiceRegistry.getApplicationServiceIdempotentWrapper().idempotent(event.getId().toString(), (ignored) -> {
            log.debug("handle new project role created event");
            RoleId adminRoleId = new RoleId(event.getDomainId().getDomainId());
            RoleId userRoleId = event.getUserRoleId();
            UserId creator = event.getCreator();
            ProjectId tenantId = event.getProjectId();
            UserRelation.onboardNewProject(adminRoleId, userRoleId, creator, tenantId, new ProjectId(AppConstant.MT_AUTH_PROJECT_ID));
            return null;
        }, USER_RELATION);
    }

    @SubscribeForEvent
    @Transactional
    public UserRelation onboardUserToTenant(UserId userId, ProjectId projectId) {
        Optional<Role> first = DomainRegistry.getRoleRepository().getByQuery(new RoleQuery(projectId, PROJECT_USER)).findFirst();
        if (first.isEmpty())
            throw new IllegalArgumentException("unable to find default user role for project");
        return UserRelation.initNewUser(first.get().getRoleId(), userId, projectId);
    }

    public SumPagedRep<User> tenantUsers(String queryParam, String pageParam, String config) {
        UserRelationQuery userRelationQuery = new UserRelationQuery(queryParam, pageParam, config);
        DomainRegistry.getPermissionCheckService().canAccess(userRelationQuery.getProjectIds(), VIEW_TENANT_USER_SUMMARY);
        SumPagedRep<UserRelation> byQuery = DomainRegistry.getUserRelationRepository().getByQuery(userRelationQuery);
        Set<UserId> collect = byQuery.getData().stream().map(UserRelation::getUserId).collect(Collectors.toSet());
        UserQuery userQuery = new UserQuery(collect);
        return DomainRegistry.getUserRepository().usersOfQuery(userQuery);
    }


    @SubscribeForEvent
    @Transactional
    public void update(String projectId, String userId, UpdateUserRelationCommand command) {
        ProjectId projectId1 = new ProjectId(projectId);
        DomainRegistry.getPermissionCheckService().canAccess(projectId1, EDIT_TENANT_USER);
        Optional<UserRelation> byUserIdAndProjectId = DomainRegistry.getUserRelationRepository().getByUserIdAndProjectId(new UserId(userId), projectId1);
        Set<RoleId> collect = command.getRoles().stream().map(RoleId::new).collect(Collectors.toSet());
        if (collect.size() > 0) {
            Set<Role> allByQuery = QueryUtility.getAllByQuery(e -> DomainRegistry.getRoleRepository().getByQuery((RoleQuery) e), new RoleQuery(collect));
            //remove default user so mt-auth will not be miss added to tenant list
            Set<Role> removeDefaultUser = allByQuery.stream().filter(e -> !AppConstant.MT_AUTH_DEFAULT_USER_ROLE.equals(e.getRoleId().getDomainId())).collect(Collectors.toSet());
            Set<ProjectId> collect1 = removeDefaultUser.stream().map(Role::getTenantId).collect(Collectors.toSet());
            //update tenant list based on role selected
            byUserIdAndProjectId.ifPresent(e -> {
                e.setStandaloneRoles(command.getRoles().stream().map(RoleId::new).collect(Collectors.toSet()));
                e.setTenantIds(collect1);
            });

        } else {
            byUserIdAndProjectId.ifPresent(e -> {
                e.setStandaloneRoles(Collections.emptySet());
                e.setTenantIds(Collections.emptySet());
            });
        }
    }

    public Optional<UserTenantRepresentation> tenantUserDetail(String projectId, String id) {
        UserRelationQuery userRelationQuery = new UserRelationQuery(new UserId(id), new ProjectId(projectId));
        DomainRegistry.getPermissionCheckService().canAccess(userRelationQuery.getProjectIds(), VIEW_TENANT_USER);
        return DomainRegistry.getUserRelationRepository().getByQuery(userRelationQuery).findFirst().map(userRelation -> {
            UserQuery userQuery = new UserQuery(userRelation.getUserId());
            return DomainRegistry.getUserRepository().usersOfQuery(userQuery).findFirst().map(e -> new UserTenantRepresentation(userRelation, e));
        }).orElseGet(Optional::empty);
    }

    public Optional<User> myProfile() {
        UserId userId = DomainRegistry.getCurrentUserService().getUserId();
        return DomainRegistry.getUserRepository().userOfId(userId);
    }
}
