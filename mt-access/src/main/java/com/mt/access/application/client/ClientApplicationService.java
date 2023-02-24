package com.mt.access.application.client;

import static com.mt.access.domain.model.permission.Permission.CREATE_CLIENT;
import static com.mt.access.domain.model.permission.Permission.EDIT_CLIENT;
import static com.mt.access.domain.model.permission.Permission.VIEW_CLIENT;

import com.github.fge.jsonpatch.JsonPatch;
import com.mt.access.application.client.command.ClientCreateCommand;
import com.mt.access.application.client.command.ClientPatchCommand;
import com.mt.access.application.client.command.ClientUpdateCommand;
import com.mt.access.application.client.representation.ClientSpringOAuth2Representation;
import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.audit.AuditLog;
import com.mt.access.domain.model.client.Client;
import com.mt.access.domain.model.client.ClientId;
import com.mt.access.domain.model.client.ClientQuery;
import com.mt.access.domain.model.client.RedirectDetail;
import com.mt.access.domain.model.client.TokenDetail;
import com.mt.access.domain.model.client.event.ClientAsResourceDeleted;
import com.mt.access.domain.model.client.event.ClientResourceCleanUpCompleted;
import com.mt.access.domain.model.endpoint.Endpoint;
import com.mt.access.domain.model.endpoint.EndpointQuery;
import com.mt.access.domain.model.permission.PermissionId;
import com.mt.access.domain.model.project.ProjectId;
import com.mt.access.domain.model.role.Role;
import com.mt.access.domain.model.role.RoleQuery;
import com.mt.access.domain.model.role.event.ExternalPermissionUpdated;
import com.mt.common.application.CommonApplicationServiceRegistry;
import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.domain_event.DomainId;
import com.mt.common.domain.model.exception.DefinedRuntimeException;
import com.mt.common.domain.model.exception.ExceptionCatalog;
import com.mt.common.domain.model.exception.HttpResponseCode;
import com.mt.common.domain.model.restful.SumPagedRep;
import com.mt.common.domain.model.restful.query.QueryUtility;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ClientApplicationService implements ClientDetailsService {

    public static final String CLIENT = "Client";

    @Transactional
    @AuditLog(actionName = "create client")
    public String tenantCreate(ClientCreateCommand command, String operationId) {
        ClientId clientId = new ClientId();
        DomainRegistry.getPermissionCheckService()
            .canAccess(new ProjectId(command.getProjectId()), CREATE_CLIENT);
        return CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(operationId,
                (change) -> {
                    Client client = new Client(
                        clientId,
                        new ProjectId(command.getProjectId()),
                        command.getName(),
                        command.getPath(),
                        command.getClientSecret(),
                        command.getDescription(),
                        command.isResourceIndicator(),
                        command.getResourceIds() != null
                            ? command.getResourceIds().stream().map(ClientId::new)
                            .collect(Collectors.toSet()) : Collections.emptySet(),
                        command.getGrantTypeEnums(),
                        new TokenDetail(command.getAccessTokenValiditySeconds(),
                            command.getRefreshTokenValiditySeconds()),
                        new RedirectDetail(
                            command.getRegisteredRedirectUri(),
                            command.isAutoApprove()
                        ),
                        command.getTypes()
                    );
                    return client.getClientId().getDomainId();
                }, CLIENT
            );

    }

    public SumPagedRep<Client> tenantQuery(String queryParam, String pagingParam,
                                           String configParam) {
        ClientQuery clientQuery = new ClientQuery(queryParam, pagingParam, configParam);
        DomainRegistry.getPermissionCheckService()
            .canAccess(clientQuery.getProjectIds(), VIEW_CLIENT);
        return DomainRegistry.getClientRepository().clientsOfQuery(clientQuery);
    }

    public Optional<Client> tenantQuery(String id, String projectId) {
        ClientQuery clientQuery = new ClientQuery(new ClientId(id), new ProjectId(projectId));
        DomainRegistry.getPermissionCheckService()
            .canAccess(clientQuery.getProjectIds(), VIEW_CLIENT);
        return DomainRegistry.getClientRepository().clientsOfQuery(clientQuery).findFirst();
    }

    public SumPagedRep<Client> adminQuery(String queryParam, String pagingParam,
                                          String configParam) {
        ClientQuery clientQuery = new ClientQuery(queryParam, pagingParam, configParam);
        return DomainRegistry.getClientRepository().clientsOfQuery(clientQuery);
    }

    public Optional<Client> adminQueryById(String id) {
        ClientQuery clientQuery = new ClientQuery(new ClientId(id));
        return DomainRegistry.getClientRepository().clientsOfQuery(clientQuery).findFirst();
    }

    public SumPagedRep<Client> internalQuery(String pagingParam, String configParam) {
        return DomainRegistry.getClientRepository()
            .clientsOfQuery(ClientQuery.internalQuery(pagingParam, configParam));
    }


    public Optional<Client> internalQuery(ClientId id) {
        return DomainRegistry.getClientRepository().clientOfId(id);
    }

    @Transactional
    @AuditLog(actionName = "update client")
    public void tenantReplace(String id, ClientUpdateCommand command, String changeId) {
        ClientId clientId = new ClientId(id);
        DomainRegistry.getPermissionCheckService()
            .canAccess(new ProjectId(command.getProjectId()), EDIT_CLIENT);
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(changeId, (ignored) -> {
                ClientQuery clientQuery =
                    new ClientQuery(clientId, new ProjectId(command.getProjectId()));
                Optional<Client> optionalClient =
                    DomainRegistry.getClientRepository().clientsOfQuery(clientQuery).findFirst();
                if (optionalClient.isPresent()) {
                    Client client = optionalClient.get();
                    client.replace(
                        command.getName(),
                        command.getClientSecret(),
                        command.getPath(),
                        command.getDescription(),
                        command.isResourceIndicator(),
                        command.getResourceIds() != null
                            ?
                            command.getResourceIds().stream().map(ClientId::new)
                                .collect(Collectors.toSet())
                            : Collections.emptySet(),
                        command.getGrantTypeEnums(),
                        new TokenDetail(command.getAccessTokenValiditySeconds(),
                            command.getRefreshTokenValiditySeconds()),
                        new RedirectDetail(
                            command.getRegisteredRedirectUri(),
                            command.isAutoApprove()
                        )
                    );
                    DomainRegistry.getClientRepository().add(client);
                }
                return null;
            }, CLIENT);
    }

    @Transactional
    @AuditLog(actionName = "remove client")
    public void tenantRemove(String projectId, String id, String changeId) {
        ClientId clientId = new ClientId(id);
        DomainRegistry.getPermissionCheckService().canAccess(new ProjectId(projectId), EDIT_CLIENT);
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(changeId, (change) -> {
                ClientQuery clientQuery = new ClientQuery(clientId, new ProjectId(projectId));
                Optional<Client> client =
                    DomainRegistry.getClientRepository().clientsOfQuery(clientQuery).findFirst();
                if (client.isPresent()) {
                    Client client1 = client.get();
                    if (client1.removable()) {
                        DomainRegistry.getClientRepository().remove(client1);
                        client1.removeAllReferenced();
                    } else {
                        throw new DefinedRuntimeException("root client cannot be deleted", "0009",
                            HttpResponseCode.BAD_REQUEST,
                            ExceptionCatalog.ILLEGAL_ARGUMENT);
                    }
                }
                return null;
            }, CLIENT);
    }

    @Transactional
    @AuditLog(actionName = "patch client")
    public void patch(String projectId, String id, JsonPatch command, String changeId) {
        ProjectId projectId1 = new ProjectId(projectId);
        DomainRegistry.getPermissionCheckService().canAccess(projectId1, EDIT_CLIENT);
        ClientId clientId = new ClientId(id);
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(changeId, (ignored) -> {
                ClientQuery clientQuery = new ClientQuery(clientId, projectId1);
                Optional<Client> client =
                    DomainRegistry.getClientRepository().clientsOfQuery(clientQuery).findFirst();
                if (client.isPresent()) {
                    Client original = client.get();
                    ClientPatchCommand beforePatch = new ClientPatchCommand(original);
                    ClientPatchCommand afterPatch = CommonDomainRegistry.getCustomObjectSerializer()
                        .applyJsonPatch(command, beforePatch, ClientPatchCommand.class);
                    original.replace(
                        afterPatch.getName(),
                        null,
                        afterPatch.getPath(),
                        afterPatch.getDescription(),
                        afterPatch.isResourceIndicator(),
                        afterPatch.getResourceIds() != null
                            ?
                            afterPatch.getResourceIds().stream().map(ClientId::new)
                                .collect(Collectors.toSet()) : Collections.emptySet(),
                        afterPatch.getGrantTypeEnums(),
                        new TokenDetail(afterPatch.getAccessTokenValiditySeconds(),
                            original.getTokenDetail().getRefreshTokenValiditySeconds()),
                        original.getAuthorizationCodeGrant()
                    );
                }
                return null;
            }, CLIENT);
    }

    @Override
    public ClientDetails loadClientByClientId(String id) throws ClientRegistrationException {
        Optional<Client> client = DomainRegistry.getClientRepository().clientOfId(new ClientId(id));
        return client.map(ClientSpringOAuth2Representation::new).orElse(null);
    }

    @Transactional
    public void handle(ClientAsResourceDeleted deserialize) {
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(deserialize.getId().toString(), (ignored) -> {
                //remove deleted client from resource_map
                DomainId domainId = deserialize.getDomainId();
                ClientId removedClientId = new ClientId(domainId.getDomainId());
                Set<Client> allByQuery = QueryUtility.getAllByQuery(
                    (query) -> DomainRegistry.getClientRepository().clientsOfQuery(query),
                    ClientQuery.resourceIds(removedClientId));
                allByQuery.forEach(e -> e.removeResource(removedClientId));
                Set<ClientId> collect =
                    allByQuery.stream().map(Client::getClientId).collect(Collectors.toSet());
                collect.add(removedClientId);
                CommonDomainRegistry.getDomainEventRepository()
                    .append(new ClientResourceCleanUpCompleted(collect));
                return null;
            }, CLIENT);
    }

    /**
     * update project all client's external resource list after project external permission updated.
     *
     * @param event ExternalPermissionUpdated
     */
    @Transactional
    public void handle(ExternalPermissionUpdated event) {
        CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(event.getId().toString(), (ignored) -> {
                ProjectId projectId = new ProjectId(event.getDomainId().getDomainId());
                Set<Client> projectClients = QueryUtility
                    .getAllByQuery(e -> DomainRegistry.getClientRepository().clientsOfQuery(e),
                        new ClientQuery(projectId));
                Set<Role> allRoles = QueryUtility
                    .getAllByQuery(e -> DomainRegistry.getRoleRepository().getByQuery(e),
                        new RoleQuery(projectId));
                Set<PermissionId> externalPermissions =
                    allRoles.stream().filter(e -> e.getExternalPermissionIds() != null)
                        .flatMap(e -> e.getExternalPermissionIds().stream())
                        .collect(Collectors.toSet());
                Set<Endpoint> referredClients = QueryUtility.getAllByQuery(e ->
                        DomainRegistry.getEndpointRepository().endpointsOfQuery(e),
                    EndpointQuery.permissionQuery(externalPermissions));
                Set<ClientId> collect =
                    referredClients.stream().map(Endpoint::getClientId).collect(Collectors.toSet());
                projectClients.forEach(client -> client.updateExternalResource(collect));
                return null;
            }, CLIENT);
    }

    public Optional<Client> canAutoApprove(String projectId, String id) {
        ClientQuery clientQuery = new ClientQuery(new ClientId(id), new ProjectId(projectId));
        return DomainRegistry.getClientRepository().clientsOfQuery(clientQuery).findFirst();
    }


    public Set<Client> findAllByIds(Set<ClientId> ids) {
        return QueryUtility
            .getAllByQuery(e -> DomainRegistry.getClientRepository().clientsOfQuery(e),
                new ClientQuery(ids));
    }
}
