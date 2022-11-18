package com.mt.access.domain.model.permission;

import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.endpoint.EndpointId;
import com.mt.access.domain.model.permission.event.ProjectPermissionCreated;
import com.mt.access.domain.model.project.ProjectId;
import com.mt.access.domain.model.user.UserId;
import com.mt.access.infrastructure.AppConstant;
import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.audit.Auditable;
import com.mt.common.infrastructure.HttpValidationNotificationHandler;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Table
@Entity
@NoArgsConstructor
@Getter
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE,
    region = "permissionRegion")
public class Permission extends Auditable {
    public static final String API_ACCESS = "API_ACCESS";
    public static final String VIEW_PROJECT_INFO = "VIEW_PROJECT_INFO";
    public static final String VIEW_CLIENT = "VIEW_CLIENT";
    public static final String EDIT_CLIENT = "EDIT_CLIENT";
    public static final String CREATE_CLIENT = "CREATE_CLIENT";
    public static final String VIEW_API = "VIEW_API";
    public static final String EDIT_API = "EDIT_API";
    public static final String CREATE_API = "CREATE_API";
    public static final String CREATE_PERMISSION = "CREATE_PERMISSION";
    public static final String VIEW_PERMISSION = "VIEW_PERMISSION";
    public static final String EDIT_PERMISSION = "EDIT_PERMISSION";
    public static final String VIEW_TENANT_USER = "VIEW_TENANT_USER";
    public static final String EDIT_TENANT_USER = "EDIT_TENANT_USER";
    public static final String EDIT_ROLE = "EDIT_ROLE";
    public static final String CREATE_ROLE = "CREATE_ROLE";
    public static final String VIEW_ROLE = "VIEW_ROLE";
    public static final String USER_MNGMT = "USER_MNGMT";
    public static final String PERMISSION_MNGMT = "PERMISSION_MNGMT";
    public static final String ROLE_MNGMT = "ROLE_MNGMT";
    public static final String API_MNGMT = "API_MNGMT";
    public static final String CLIENT_MNGMT = "CLIENT_MNGMT";
    public static final String EDIT_PROJECT_INFO = "EDIT_PROJECT_INFO";
    public static final String PROJECT_INFO_MNGMT = "PROJECT_INFO_MNGMT";
    public static final String SUB_REQ_MNGMT = "SUB_REQ_MNGMT";
    public static final Set<String> reservedName = new HashSet<>();
    public static final Set<String> reservedUIPermissionName = new HashSet<>();

    static {
        reservedName.add(API_ACCESS);
        reservedUIPermissionName.add(SUB_REQ_MNGMT);
        reservedUIPermissionName.add(VIEW_PROJECT_INFO);
        reservedUIPermissionName.add(VIEW_CLIENT);
        reservedUIPermissionName.add(EDIT_CLIENT);
        reservedUIPermissionName.add(CREATE_CLIENT);
        reservedUIPermissionName.add(VIEW_API);
        reservedUIPermissionName.add(EDIT_API);
        reservedUIPermissionName.add(CREATE_API);
        reservedUIPermissionName.add(CREATE_PERMISSION);
        reservedUIPermissionName.add(VIEW_PERMISSION);
        reservedUIPermissionName.add(EDIT_PERMISSION);
        reservedUIPermissionName.add(VIEW_TENANT_USER);
        reservedUIPermissionName.add(EDIT_TENANT_USER);
        reservedUIPermissionName.add(EDIT_ROLE);
        reservedUIPermissionName.add(CREATE_ROLE);
        reservedUIPermissionName.add(VIEW_ROLE);
        reservedUIPermissionName.add(USER_MNGMT);
        reservedUIPermissionName.add(PERMISSION_MNGMT);
        reservedUIPermissionName.add(ROLE_MNGMT);
        reservedUIPermissionName.add(API_MNGMT);
        reservedUIPermissionName.add(CLIENT_MNGMT);
        reservedUIPermissionName.add(EDIT_PROJECT_INFO);
        reservedUIPermissionName.add(PROJECT_INFO_MNGMT);
        reservedName.addAll(reservedUIPermissionName);

    }

    private String name;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "domainId", column = @Column(name = "parentId"))
    })
    private PermissionId parentId;

    @Embedded
    private PermissionId permissionId;
    @Getter
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "linked_permission_ids_map",
        joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"id", "domainId"})
    )
    @AttributeOverrides({
        @AttributeOverride(name = "domainId", column = @Column(updatable = false, nullable = false))
    })
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE,
        region = "linkedPermissionIdsRegion")
    private Set<PermissionId> linkedApiPermissionIds;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "domainId", column = @Column(name = "projectId"))
    })
    private ProjectId projectId;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "domainId", column = @Column(name = "tenantId"))
    })
    private ProjectId tenantId;
    @Setter
    private boolean shared = false;
    @Convert(converter = PermissionType.DbConverter.class)
    private PermissionType type;
    private boolean systemCreate = false;

    private Permission(ProjectId projectId, PermissionId permissionId, String name,
                       PermissionType type, @Nullable PermissionId parentId,
                       @Nullable ProjectId tenantId,
                       @Nullable Set<PermissionId> linkedApiPermissionIds, boolean shared) {
        super();
        this.id = CommonDomainRegistry.getUniqueIdGeneratorService().id();
        this.permissionId = permissionId;
        this.linkedApiPermissionIds = linkedApiPermissionIds;
        this.parentId = parentId;
        this.projectId = projectId;
        this.tenantId = tenantId;
        this.name = name;
        this.type = type;
        this.shared = shared;
    }

    /**
     * create api with one linked api permission
     *
     * @param projectId             project id
     * @param permissionId          new permission id
     * @param name                  permission name
     * @param type                  permission type
     * @param parentId              parent permission id if exist
     * @param tenantId              tenant project id if exist
     * @param linkedApiPermissionId linked api permission id
     * @return permission
     */
    private static Permission autoCreateForProject(ProjectId projectId, PermissionId permissionId,
                                                   String name, PermissionType type,
                                                   @Nullable PermissionId parentId,
                                                   @Nullable ProjectId tenantId,
                                                   @Nullable PermissionId linkedApiPermissionId) {
        Permission permission =
            new Permission(projectId, permissionId, name, type, parentId, tenantId,
                Stream.of(linkedApiPermissionId).collect(Collectors.toSet()), false);
        permission.systemCreate = true;
        new PermissionValidator(new HttpValidationNotificationHandler(), permission).validate();
        return permission;
    }

    /**
     * create api with multiple linked api permission
     *
     * @param projectId             project id
     * @param permissionId          new permission id
     * @param name                  permission name
     * @param parentId              parent permission id if exist
     * @param tenantId              tenant project id if exist
     * @param linkedApiPermissionId linked api permission id
     * @return permission
     */
    private static Permission autoCreateForProjectMulti(
        ProjectId projectId,
        PermissionId permissionId, String name,
        @Nullable PermissionId parentId,
        @Nullable ProjectId tenantId,
        @Nullable Set<PermissionId> linkedApiPermissionId
    ) {
        Permission permission =
            new Permission(projectId, permissionId, name, PermissionType.COMMON, parentId, tenantId,
                linkedApiPermissionId, false);
        permission.systemCreate = true;
        new PermissionValidator(new HttpValidationNotificationHandler(), permission).validate();
        return permission;
    }

    /**
     * used for auto create permission for new endpoint
     *
     * @param projectId    project id
     * @param permissionId permission id
     * @param apiDomainId  endpoint id
     * @param parentId     api root
     * @param shared       if api is shared
     * @return permission aggregate
     */
    private static Permission autoCreateForEndpoint(ProjectId projectId, PermissionId permissionId,
                                                    String apiDomainId,
                                                    @Nullable PermissionId parentId,
                                                    boolean shared) {
        Permission permission =
            new Permission(projectId, permissionId, apiDomainId, PermissionType.API, parentId, null,
                Collections.singleton(null), shared);
        permission.systemCreate = true;
        new PermissionValidator(new HttpValidationNotificationHandler(), permission).validate();
        return permission;
    }

    public static Permission manualCreate(ProjectId projectId, PermissionId permissionId,
                                          String name, PermissionType type,
                                          @Nullable PermissionId parentId,
                                          @Nullable ProjectId tenantId,
                                          @Nullable Set<PermissionId> linkedApiPermissionId) {
        Permission permission =
            new Permission(projectId, permissionId, name, type, parentId, tenantId,
                linkedApiPermissionId, false);
        new PermissionValidator(new HttpValidationNotificationHandler(), permission).validate();
        return permission;
    }

    /**
     * create permissions for new project
     *
     * @param tenantId  tenant project that is creating
     * @param creatorId user id who create project
     */
    public static void onboardNewProject(ProjectId tenantId,
                                         UserId creatorId) {
        ProjectId projectId = new ProjectId(AppConstant.MT_AUTH_PROJECT_ID);
        PermissionId rootId = new PermissionId();
        Permission p0 = Permission
            .autoCreateForProject(projectId, rootId, tenantId.getDomainId(), PermissionType.PROJECT,
                null, tenantId, null);
        PermissionId projectMgntId = new PermissionId();
        Permission p1 = Permission
            .autoCreateForProject(projectId, projectMgntId, PROJECT_INFO_MNGMT,
                PermissionType.COMMON, rootId, tenantId, null);
        Permission p2 = Permission
            .autoCreateForProject(projectId, new PermissionId(), VIEW_PROJECT_INFO,
                PermissionType.COMMON, projectMgntId, tenantId, new PermissionId("0Y8HSHJC34BW"));
        Permission p3 = Permission
            .autoCreateForProject(projectId, new PermissionId(), EDIT_PROJECT_INFO,
                PermissionType.COMMON, projectMgntId, tenantId, null);
        PermissionId clientMgntId = new PermissionId();
        Permission p4 = Permission
            .autoCreateForProject(projectId, clientMgntId, CLIENT_MNGMT, PermissionType.COMMON,
                rootId, tenantId, null);
        Permission p5 = Permission
            .autoCreateForProject(projectId, new PermissionId(), CREATE_CLIENT,
                PermissionType.COMMON, clientMgntId, tenantId, new PermissionId("0Y8HHJ47NBD6"));
        Permission p6 = Permission
            .autoCreateForProjectMulti(projectId, new PermissionId(), VIEW_CLIENT,
                clientMgntId, tenantId,
                Stream.of(new PermissionId("0Y8HHJ47NBDP"), new PermissionId("0Y8HHJ47NBD4"))
                    .collect(Collectors.toSet()));
        Permission p7 = Permission
            .autoCreateForProjectMulti(projectId, new PermissionId(), EDIT_CLIENT,
                clientMgntId, tenantId, Stream
                    .of(new PermissionId("0Y8HHJ47NBD8"), new PermissionId("0Y8HHJ47NBD7"),
                        new PermissionId("0Y8HHJ47NBDQ")).collect(Collectors.toSet()));

        PermissionId apiMgntId = new PermissionId();
        Permission p11 = Permission
            .autoCreateForProject(projectId, apiMgntId, API_MNGMT, PermissionType.COMMON, rootId,
                tenantId, null);
        Permission p13 = Permission
            .autoCreateForProjectMulti(projectId, new PermissionId(), VIEW_API,
                apiMgntId, tenantId, Stream
                    .of(new PermissionId("0Y8HHJ47NBEM"), new PermissionId("0Y8HHJ47NBEH"),
                        new PermissionId("0Y8HHJ47NBDS"), new PermissionId("0Y8HHJ47NBDM"))
                    .collect(Collectors.toSet()));
        Permission p14 = Permission
            .autoCreateForProjectMulti(projectId, new PermissionId(), EDIT_API,
                apiMgntId, tenantId, Stream
                    .of(new PermissionId("0Y8HHJ47NBDV"), new PermissionId("0Y8HHJ47NBDN"),
                        new PermissionId("0Y8HHJ47NBDO"), new PermissionId("0Y8HHJ47NBDW"))
                    .collect(Collectors.toSet()));
        Permission p16 = Permission
            .autoCreateForProject(projectId, new PermissionId(), CREATE_API, PermissionType.COMMON,
                apiMgntId, tenantId, new PermissionId("0Y8HHJ47NBDL"));

        PermissionId roleMgntId = new PermissionId();
        Permission p19 = Permission
            .autoCreateForProject(projectId, roleMgntId, ROLE_MNGMT, PermissionType.COMMON, rootId,
                tenantId, null);
        Permission p21 = Permission
            .autoCreateForProjectMulti(projectId, new PermissionId(), EDIT_ROLE,
                roleMgntId, tenantId,
                Stream.of(new PermissionId("0Y8HKE2QAIVF"), new PermissionId("0Y8HKE24FWUI"))
                    .collect(Collectors.toSet()));
        Permission p22 = Permission
            .autoCreateForProject(projectId, new PermissionId(), CREATE_ROLE, PermissionType.COMMON,
                roleMgntId, tenantId, new PermissionId("0Y8HHJ47NBEY"));
        Permission p23 = Permission
            .autoCreateForProjectMulti(projectId, new PermissionId(), VIEW_ROLE,
                roleMgntId, tenantId,
                Stream.of(new PermissionId("0Y8HHJ47NBEX"), new PermissionId("0Y8HKACDVMDL"))
                    .collect(Collectors.toSet()));

        //permission mngmnt related permission
        PermissionId permissionMgntId = new PermissionId();
        Permission p25 = Permission
            .autoCreateForProject(projectId, permissionMgntId, PERMISSION_MNGMT,
                PermissionType.COMMON, rootId, tenantId, null);
        Permission p26 = Permission
            .autoCreateForProject(projectId, new PermissionId(), CREATE_PERMISSION,
                PermissionType.COMMON, permissionMgntId, tenantId,
                new PermissionId("0Y8HHJ47NBEW"));
        Permission p28 = Permission
            .autoCreateForProjectMulti(projectId, new PermissionId(), VIEW_PERMISSION,
                permissionMgntId, tenantId,
                Stream.of(new PermissionId("0Y8HHJ47NBEV"), new PermissionId("0Y8HLUWG1UJ8"))
                    .collect(Collectors.toSet()));
        Permission p29 = Permission
            .autoCreateForProjectMulti(projectId, new PermissionId(), EDIT_PERMISSION,
                permissionMgntId, tenantId, Stream
                    .of(new PermissionId("0Y8HLUWKQEJ1"), new PermissionId("0Y8HLUWOH91P"),
                        new PermissionId("0Y8HLUWMX2BX")).collect(Collectors.toSet()));
        //position mngmnt related permission
        PermissionId positionMgntId = new PermissionId();
        Permission p32 = Permission
            .autoCreateForProject(projectId, positionMgntId, USER_MNGMT, PermissionType.COMMON,
                rootId, tenantId, null);
        Permission p34 = Permission
            .autoCreateForProjectMulti(projectId, new PermissionId(), VIEW_TENANT_USER,
                positionMgntId, tenantId,
                Stream.of(new PermissionId("0Y8HK4ZLA03Q"), new PermissionId("0Y8HKEMUH34B"))
                    .collect(Collectors.toSet()));
        Permission p35 = Permission
            .autoCreateForProject(projectId, new PermissionId(), EDIT_TENANT_USER,
                PermissionType.COMMON, positionMgntId, tenantId, new PermissionId("0Y8HKEMWNQX7"));
        //sub request mngmnt related permission
        PermissionId subReqMgntId = new PermissionId();
        Permission p36 = Permission
            .autoCreateForProjectMulti(projectId, subReqMgntId, SUB_REQ_MNGMT,
                rootId, tenantId, Stream.of(
                    new PermissionId("0Y8M0IG8RITC"),
                    new PermissionId("0Y8M0IQAUSZ8"),
                    new PermissionId("0Y8M0IQQ5FK0"),
                    new PermissionId("0Y8M0IR20GBI"),
                    new PermissionId("0Y8M0IRD8ZSN"),
                    new PermissionId("0Y8M4M3J9HJ4"),
                    new PermissionId("0Y8M0IRN8L4W")
                )
                    .collect(Collectors.toSet()));

        Permission apiPermission = Permission
            .autoCreateForProject(tenantId, new PermissionId(), API_ACCESS, PermissionType.API_ROOT,
                null, null, null);

        DomainRegistry.getPermissionRepository().add(apiPermission);
        DomainRegistry.getPermissionRepository().add(p0);
        DomainRegistry.getPermissionRepository().add(p1);
        DomainRegistry.getPermissionRepository().add(p2);
        DomainRegistry.getPermissionRepository().add(p3);
        DomainRegistry.getPermissionRepository().add(p4);
        DomainRegistry.getPermissionRepository().add(p5);
        DomainRegistry.getPermissionRepository().add(p6);
        DomainRegistry.getPermissionRepository().add(p7);
        DomainRegistry.getPermissionRepository().add(p11);
        DomainRegistry.getPermissionRepository().add(p13);
        DomainRegistry.getPermissionRepository().add(p14);
        DomainRegistry.getPermissionRepository().add(p16);
        DomainRegistry.getPermissionRepository().add(p19);
        DomainRegistry.getPermissionRepository().add(p21);
        DomainRegistry.getPermissionRepository().add(p22);
        DomainRegistry.getPermissionRepository().add(p23);
        DomainRegistry.getPermissionRepository().add(p25);
        DomainRegistry.getPermissionRepository().add(p26);
        DomainRegistry.getPermissionRepository().add(p28);
        DomainRegistry.getPermissionRepository().add(p29);
        DomainRegistry.getPermissionRepository().add(p32);
        DomainRegistry.getPermissionRepository().add(p34);
        DomainRegistry.getPermissionRepository().add(p35);
        DomainRegistry.getPermissionRepository().add(p36);
        Set<Permission> createdPermissions = new HashSet<>();
        createdPermissions.add(p0);
        createdPermissions.add(p1);
        createdPermissions.add(p2);
        createdPermissions.add(p3);
        createdPermissions.add(p4);
        createdPermissions.add(p5);
        createdPermissions.add(p6);
        createdPermissions.add(p7);
        createdPermissions.add(p11);
        createdPermissions.add(p13);
        createdPermissions.add(p14);
        createdPermissions.add(p16);
        createdPermissions.add(p19);
        createdPermissions.add(p21);
        createdPermissions.add(p22);
        createdPermissions.add(p23);
        createdPermissions.add(p25);
        createdPermissions.add(p26);
        createdPermissions.add(p28);
        createdPermissions.add(p29);
        createdPermissions.add(p32);
        createdPermissions.add(p34);
        createdPermissions.add(p35);
        createdPermissions.add(p36);
        Set<PermissionId> collect = createdPermissions.stream().flatMap(e -> {
            if (e.getLinkedApiPermissionIds() != null && !e.getLinkedApiPermissionIds().isEmpty()) {
                e.getLinkedApiPermissionIds().add(e.getPermissionId());
                return e.linkedApiPermissionIds.stream();
            } else {
                return Stream.of(e.getPermissionId());
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
        CommonDomainRegistry.getDomainEventRepository()
            .append(new ProjectPermissionCreated(collect, tenantId, creatorId));
    }

    public static void addNewEndpoint(ProjectId projectId, EndpointId endpointId,
                                      PermissionId permissionId, boolean shared) {
        Optional<Permission> apiRoot = DomainRegistry.getPermissionRepository()
            .getByQuery(new PermissionQuery(projectId, API_ACCESS)).findFirst();
        apiRoot.ifPresent(e -> {
            Permission apiPermission = Permission
                .autoCreateForEndpoint(projectId, permissionId, endpointId.getDomainId(),
                    apiRoot.get().getPermissionId(), shared);
            DomainRegistry.getPermissionRepository().add(apiPermission);
        });
    }

    public void replace(String name, Set<PermissionId> permissionIds) {
        updateName(name);
        this.linkedApiPermissionIds = permissionIds;
    }

    private void updateName(String name) {
        if (List.of(PermissionType.API, PermissionType.API_ROOT, PermissionType.PROJECT)
            .contains(this.type)) {
            throw new IllegalStateException("api, api root and project type's cannot be changed");
        }
        if (isSystemCreate()) {
            throw new IllegalStateException("system created permission cannot be changed");
        }
        this.name = name;
    }

    public void patch(String name) {
        updateName(name);
    }

    public void remove() {
        if (List.of(PermissionType.API, PermissionType.API_ROOT, PermissionType.PROJECT)
            .contains(this.type)) {
            throw new IllegalStateException("api, api root and project type's cannot be changed");
        }
        DomainRegistry.getPermissionRepository().remove(this);
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
        Permission that = (Permission) o;
        return Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), permissionId);
    }
}
