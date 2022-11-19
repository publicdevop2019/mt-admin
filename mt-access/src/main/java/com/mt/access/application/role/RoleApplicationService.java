package com.mt.access.application.role;

import static com.mt.access.domain.model.permission.Permission.CREATE_ROLE;
import static com.mt.access.domain.model.permission.Permission.EDIT_ROLE;
import static com.mt.access.domain.model.permission.Permission.VIEW_ROLE;

import com.github.fge.jsonpatch.JsonPatch;
import com.mt.access.application.ApplicationServiceRegistry;
import com.mt.access.application.role.command.RoleCreateCommand;
import com.mt.access.application.role.command.RolePatchCommand;
import com.mt.access.application.role.command.RoleUpdateCommand;
import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.audit.AuditLog;
import com.mt.access.domain.model.client.ClientId;
import com.mt.access.domain.model.client.event.ClientCreated;
import com.mt.access.domain.model.client.event.ClientDeleted;
import com.mt.access.domain.model.endpoint.event.EndpointShareRemoved;
import com.mt.access.domain.model.permission.PermissionId;
import com.mt.access.domain.model.permission.event.ProjectPermissionCreated;
import com.mt.access.domain.model.project.ProjectId;
import com.mt.access.domain.model.role.Role;
import com.mt.access.domain.model.role.RoleId;
import com.mt.access.domain.model.role.RoleQuery;
import com.mt.access.domain.model.role.RoleType;
import com.mt.access.domain.model.user.UserId;
import com.mt.access.infrastructure.AppConstant;
import com.mt.common.application.CommonApplicationServiceRegistry;
import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.distributed_lock.SagaDistLock;
import com.mt.common.domain.model.restful.SumPagedRep;
import com.mt.common.domain.model.restful.query.QueryUtility;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RoleApplicationService {
    private static final String ROLE = "Role";

    public SumPagedRep<Role> getByQuery(RoleQuery roleQuery) {
        return DomainRegistry.getRoleRepository().getByQuery(roleQuery);
    }

    public SumPagedRep<Role> getByQuery(String queryParam, String pageParam, String skipCount) {
        RoleQuery roleQuery = new RoleQuery(queryParam, pageParam, skipCount);
        DomainRegistry.getPermissionCheckService().canAccess(roleQuery.getProjectIds(), VIEW_ROLE);
        return DomainRegistry.getRoleRepository().getByQuery(roleQuery);
    }

    public Optional<Role> getById(String projectId, String id) {
        RoleQuery roleQuery = new RoleQuery(new RoleId(id), new ProjectId(projectId));
        DomainRegistry.getPermissionCheckService().canAccess(roleQuery.getProjectIds(), VIEW_ROLE);
        return DomainRegistry.getRoleRepository().getByQuery(roleQuery).findFirst();
    }

    public Optional<Role> internalGetById(RoleId id) {
        return DomainRegistry.getRoleRepository().getById(id);
    }


    @Transactional
    @AuditLog(actionName = "update role")
    public void update(String id, RoleUpdateCommand command, String changeId) {
        RoleId roleId = new RoleId(id);
        RoleQuery roleQuery = new RoleQuery(roleId, new ProjectId(command.getProjectId()));
        DomainRegistry.getPermissionCheckService().canAccess(roleQuery.getProjectIds(), EDIT_ROLE);
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(changeId, (change) -> {
                Optional<Role> first =
                    DomainRegistry.getRoleRepository().getByQuery(roleQuery).findFirst();
                first.ifPresent(e -> {
                    e.update(command);
                    DomainRegistry.getRoleRepository().add(e);
                });
                return null;
            }, ROLE);
    }

    @Transactional
    @AuditLog(actionName = "update role")
    public void patch(String projectId, String id, JsonPatch command, String changeId) {
        RoleId roleId = new RoleId(id);
        RoleQuery roleQuery = new RoleQuery(roleId, new ProjectId(projectId));
        DomainRegistry.getPermissionCheckService().canAccess(roleQuery.getProjectIds(), EDIT_ROLE);
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(changeId, (change) -> {
                Optional<Role> first =
                    DomainRegistry.getRoleRepository().getByQuery(roleQuery).findFirst();
                first.ifPresent(e -> {
                    RolePatchCommand beforePatch = new RolePatchCommand(e);
                    RolePatchCommand afterPatch =
                        CommonDomainRegistry.getCustomObjectSerializer()
                            .applyJsonPatch(command, beforePatch, RolePatchCommand.class);
                    e.patch(afterPatch.getName());
                    DomainRegistry.getRoleRepository().add(e);
                });
                return null;
            }, ROLE);
    }


    @Transactional
    @AuditLog(actionName = "remove role")
    public void remove(String projectId, String id, String changeId) {
        RoleId roleId = new RoleId(id);
        RoleQuery roleQuery = new RoleQuery(roleId, new ProjectId(projectId));
        DomainRegistry.getPermissionCheckService().canAccess(roleQuery.getProjectIds(), EDIT_ROLE);
        CommonApplicationServiceRegistry.getIdempotentService().idempotent(changeId, (ignored) -> {
            Optional<Role> role = DomainRegistry.getRoleRepository().getById(roleId);
            role.ifPresent(Role::remove);
            return null;
        }, ROLE);
    }

    /**
     * create role, permissions must belong to root node.
     *
     * @param command  create command
     * @param changeId change id
     * @return role created id
     */

    @Transactional
    @AuditLog(actionName = "create role")
    public String create(RoleCreateCommand command, String changeId) {
        RoleId roleId = new RoleId();
        DomainRegistry.getPermissionCheckService()
            .canAccess(new ProjectId(command.getProjectId()), CREATE_ROLE);
        return CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(changeId, (change) -> {
                Role role = Role.createRoleForTenant(
                    new ProjectId(command.getProjectId()),
                    roleId,
                    command.getName(),
                    command.getDescription(),
                    command.getCommonPermissionIds().stream().map(PermissionId::new)
                        .collect(Collectors.toSet()),
                    command.getApiPermissionIds().stream().map(PermissionId::new)
                        .collect(Collectors.toSet()),
                    RoleType.USER,
                    command.getParentId() != null ? new RoleId(command.getParentId()) : null,
                    command.getExternalPermissionIds() != null
                        ?
                        command.getExternalPermissionIds().stream().map(PermissionId::new)
                            .collect(Collectors.toSet()) : null
                );
                DomainRegistry.getRoleRepository().add(role);
                return roleId.getDomainId();
            }, ROLE);
    }

    /**
     * create admin role to mt-auth and default user role to target project.
     *
     * @param event permission created event
     */
    @Transactional
    public void handle(ProjectPermissionCreated event) {
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(event.getId().toString(), (ignored) -> {
                log.debug("handle project permission created event");
                ProjectId tenantProjectId = event.getProjectId();
                ProjectId authPId = new ProjectId(AppConstant.MT_AUTH_PROJECT_ID);
                UserId creator = event.getCreator();
                Set<PermissionId> permissionIdSet =
                    event.getDomainIds().stream().map(e -> new PermissionId(e.getDomainId()))
                        .collect(Collectors.toSet());
                Role.onboardNewProject(authPId, tenantProjectId, permissionIdSet, creator);
                return null;
            }, ROLE);
    }

    /**
     * create placeholder role when new client created,
     * use saga lock to make sure event get consumed correctly.
     * e.g client deleted consumed first then client created consumed next
     *
     * @param event client created event
     */
    @Transactional
    @SagaDistLock(keyExpression = "#p0.changeId", aggregateName = ROLE, unlockAfter = 2)
    public void handle(ClientCreated event) {
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotentMsg(event.getChangeId(), (ignored) -> {
                log.debug("handle client created event");
                ProjectId projectId = event.getProjectId();
                ClientId clientId = new ClientId(event.getDomainId().getDomainId());
                RoleId roleId = event.getRoleId();
                Set<Role> allByQuery = QueryUtility
                    .getAllByQuery(e -> DomainRegistry.getRoleRepository().getByQuery(e),
                        RoleQuery.getRootRole(projectId));
                Optional<Role> first =
                    allByQuery.stream().filter(e -> RoleType.CLIENT_ROOT.equals(e.getType()))
                        .findFirst();
                if (first.isEmpty()) {
                    throw new IllegalStateException("unable to find root client role");
                }
                Role userRole = Role.newClient(projectId, roleId, clientId.getDomainId(),
                    first.get().getRoleId());
                DomainRegistry.getRoleRepository().add(userRole);
                return null;
            }, (cmd) -> null, ROLE);
    }


    /**
     * clean up role after client delete,
     * use saga lock to make sure event get consumed correctly.
     * e.g client deleted consumed first then client created consumed next
     *
     * @param event clientDeleted event
     */
    @Transactional
    @SagaDistLock(keyExpression = "#p0.changeId", aggregateName = ROLE, unlockAfter = 2)
    public void handle(ClientDeleted event) {
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotentMsg(event.getChangeId(), (ignored) -> {
                log.debug("handle client removed event {}", event.getDomainId().getDomainId());
                ClientId clientId = new ClientId(event.getDomainId().getDomainId());
                RoleQuery roleQuery = RoleQuery.forClientId(clientId);
                Set<Role> allByQuery = QueryUtility
                    .getAllByQuery(e -> DomainRegistry.getRoleRepository().getByQuery(e),
                        roleQuery);
                log.debug("role to be removed {}", allByQuery.size());
                allByQuery.forEach(e -> DomainRegistry.getRoleRepository().remove(e));
                return null;
            }, (cmd) -> null, ROLE);
    }

    /**
     * clean up role after endpoint shared removed.
     * make sure after endpoint shared removed, all referred role has this permission removed
     *
     * @param event EndpointSharedRemoved event
     */
    @Transactional
    public void handle(EndpointShareRemoved event) {
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(event.getId().toString(), (ignored) -> {
                log.debug("handle endpoint shared removed event");
                PermissionId permissionId = event.getPermissionId();
                RoleQuery roleQuery = new RoleQuery(permissionId);
                Set<Role> allByQuery = QueryUtility
                    .getAllByQuery(e -> DomainRegistry.getRoleRepository().getByQuery(e),
                        roleQuery);
                allByQuery.forEach(e -> {
                    e.removeExternalPermission(permissionId);
                    DomainRegistry.getRoleRepository().add(e);
                });
                return null;
            }, ROLE);
    }

}
