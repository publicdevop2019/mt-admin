package com.mt.access.domain.model.user;

import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.organization.OrganizationId;
import com.mt.access.domain.model.position.PositionId;
import com.mt.access.domain.model.project.Project;
import com.mt.access.domain.model.project.ProjectId;
import com.mt.access.domain.model.role.Role;
import com.mt.access.domain.model.role.RoleId;
import com.mt.access.domain.model.role.RoleQuery;
import com.mt.access.domain.model.user.event.ProjectOnboardingComplete;
import com.mt.access.infrastructure.AppConstant;
import com.mt.access.port.adapter.persistence.ProjectIdConverter;
import com.mt.access.port.adapter.persistence.RoleIdConverter;
import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.audit.Auditable;
import com.mt.common.domain.model.restful.query.QueryUtility;
import com.mt.common.domain.model.validate.Validator;
import com.mt.common.infrastructure.HttpValidationNotificationHandler;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "projectId"}))
@Entity
@NoArgsConstructor
@Getter
@Cacheable
@NamedQuery(name = "findEmailLike", query = "SELECT ur FROM UserRelation AS ur LEFT JOIN User u ON ur.userId = u.userId WHERE u.email.email LIKE :emailLike AND ur.projectId = :projectId")
@NamedQuery(name = "findEmailLikeCount", query = "SELECT COUNT(*) FROM UserRelation AS ur LEFT JOIN User u ON ur.userId = u.userId WHERE u.email.email LIKE :emailLike AND ur.projectId = :projectId")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE,
    region = "userRelationRegion")
public class UserRelation extends Auditable {
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "domainId", column = @Column(name = "userId"))
    })
    private UserId userId;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "domainId", column = @Column(name = "projectId"))
    })
    private ProjectId projectId;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "user_relation_role_map", joinColumns = @JoinColumn(name = "id"))
    @Column(name = "role")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE,
        region = "userRelationRoleRegion")
    @Convert(converter = RoleIdConverter.class)
    private Set<RoleId> standaloneRoles;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "user_relation_tenant_map", joinColumns = @JoinColumn(name = "id"))
    @Column(name = "tenant")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE,
        region = "userRelationTenantRegion")
    @Convert(converter = ProjectIdConverter.class)
    private Set<ProjectId> tenantIds;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "domainId", column = @Column(name = "organizationId"))
    })
    private OrganizationId organizationId;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "domainId", column = @Column(name = "positionId"))
    })
    private PositionId positionId;

    public UserRelation(RoleId roleId, UserId creator, ProjectId projectId, ProjectId tenantId) {
        super();
        this.id = CommonDomainRegistry.getUniqueIdGeneratorService().id();
        this.standaloneRoles = new HashSet<>();
        this.standaloneRoles.add(roleId);
        this.tenantIds = new HashSet<>();
        this.tenantIds.add(tenantId);
        this.userId = creator;
        this.projectId = projectId;
    }

    public UserRelation(RoleId roleId, UserId creator, ProjectId projectId) {
        super();
        this.id = CommonDomainRegistry.getUniqueIdGeneratorService().id();
        this.standaloneRoles = new HashSet<>();
        this.standaloneRoles.add(roleId);
        this.userId = creator;
        this.projectId = projectId;
    }

    public static void onboardNewProject(RoleId adminRoleId, RoleId userRoleId, UserId creator,
                                         ProjectId tenantId, ProjectId authProjectId) {
        //to mt-auth
        Optional<UserRelation> byUserIdAndProjectId = DomainRegistry.getUserRelationRepository()
            .query(new UserRelationQuery(creator, authProjectId)).findFirst();
        UserRelation userRelation;
        if (byUserIdAndProjectId.isPresent()) {
            userRelation = byUserIdAndProjectId.get();
            if (userRelation.tenantIds == null) {
                userRelation.tenantIds = new HashSet<>();
            }
            userRelation.tenantIds.add(tenantId);
            userRelation.standaloneRoles.add(adminRoleId);
        } else {
            userRelation = new UserRelation(adminRoleId, creator, authProjectId, tenantId);
        }
        DomainRegistry.getUserRelationRepository().add(userRelation);
        //to target project
        UserRelation userRelation2 = new UserRelation(userRoleId, creator, tenantId);
        DomainRegistry.getUserRelationRepository().add(userRelation2);
        Project project = DomainRegistry.getProjectRepository().get(tenantId);
        CommonDomainRegistry.getDomainEventRepository()
            .append(new ProjectOnboardingComplete(project));
    }

    public static UserRelation initNewUser(RoleId userRoleId, UserId creator,
                                           ProjectId authProjectId) {
        UserRelation userRelation2 = new UserRelation(userRoleId, creator, authProjectId);
        DomainRegistry.getUserRelationRepository().add(userRelation2);
        return userRelation2;
    }

    private void setStandaloneRoles(Set<RoleId> roleIds) {
        Validator.notEmpty(roleIds);
        if (!roleIds.equals(this.standaloneRoles)) {
            this.standaloneRoles.clear();
            this.standaloneRoles.addAll(roleIds);
        }
        Set<Role> allByQuery = QueryUtility
            .getAllByQuery(e -> DomainRegistry.getRoleRepository().query(e),
                new RoleQuery(roleIds));
        if (roleIds.size() != allByQuery.size()) {
            HttpValidationNotificationHandler handler = new HttpValidationNotificationHandler();
            handler.handleError("not able to find all roles");
        }
    }

    private void setTenantIds(Set<ProjectId> tenantIds) {
        if (tenantIds == null && this.tenantIds == null) {
            return;
        }
        if (tenantIds == null) {
            this.tenantIds.clear();
            return;
        }
        Set<ProjectId> collect =
            tenantIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (!collect.equals(this.tenantIds)) {
            this.tenantIds.clear();
            this.tenantIds.addAll(tenantIds);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        UserRelation that = (UserRelation) o;
        return Objects.equals(userId, that.userId) && Objects.equals(projectId, that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId, projectId);
    }

    public void addTenantAdmin(ProjectId tenantProjectId, RoleId tenantAdminRoleId) {
        if (getStandaloneRoles() == null) {
            HashSet<RoleId> roleIds = new HashSet<>();
            setStandaloneRoles(roleIds);
        }
        getStandaloneRoles().add(tenantAdminRoleId);
        if (getTenantIds() == null) {
            HashSet<ProjectId> projectIds = new HashSet<>();
            setTenantIds(projectIds);
        }
        getTenantIds().add(tenantProjectId);
    }

    public void removeTenantAdmin(ProjectId tenantProjectId, RoleId tenantAdminRoleId) {
        if (getStandaloneRoles() != null) {
            getStandaloneRoles().remove(tenantAdminRoleId);
        }
        if (getTenantIds() != null) {
            getTenantIds().remove(tenantProjectId);
        }
    }

    public void tenantUpdate(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            setStandaloneRoles(Collections.emptySet());
            setTenantIds(Collections.emptySet());
            return;
        }
        Set<RoleId> collect =
            roles.stream().map(RoleId::new).collect(Collectors.toSet());
        Set<Role> allByQuery = QueryUtility
            .getAllByQuery(e -> DomainRegistry.getRoleRepository().query(e),
                new RoleQuery(collect));
        //remove default user so mt-auth will not be miss added to tenant list
        Set<Role> removeDefaultUser = allByQuery.stream().filter(
                e -> !AppConstant.MT_AUTH_DEFAULT_USER_ROLE.equals(
                    e.getRoleId().getDomainId()))
            .collect(Collectors.toSet());
        Set<ProjectId> collect1 =
            removeDefaultUser.stream().map(Role::getTenantId)
                .collect(Collectors.toSet());
        //update tenant list based on role selected
        setStandaloneRoles(
            roles.stream().map(RoleId::new)
                .collect(Collectors.toSet()));
        setTenantIds(collect1);
    }
}
