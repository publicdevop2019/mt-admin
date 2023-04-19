package com.mt.access.resource;

import static com.mt.access.infrastructure.Utility.updateProjectIds;
import static com.mt.common.CommonConstant.HTTP_HEADER_AUTHORIZATION;
import static com.mt.common.CommonConstant.HTTP_HEADER_CHANGE_ID;
import static com.mt.common.CommonConstant.HTTP_PARAM_PAGE;
import static com.mt.common.CommonConstant.HTTP_PARAM_QUERY;
import static com.mt.common.CommonConstant.HTTP_PARAM_SKIP_COUNT;

import com.github.fge.jsonpatch.JsonPatch;
import com.mt.access.application.ApplicationServiceRegistry;
import com.mt.access.application.endpoint.command.EndpointCreateCommand;
import com.mt.access.application.endpoint.command.EndpointExpireCommand;
import com.mt.access.application.endpoint.command.EndpointUpdateCommand;
import com.mt.access.application.endpoint.representation.EndpointCardRepresentation;
import com.mt.access.application.endpoint.representation.EndpointProxyCacheRepresentation;
import com.mt.access.application.endpoint.representation.EndpointRepresentation;
import com.mt.access.application.endpoint.representation.EndpointSharedCardRepresentation;
import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.endpoint.Endpoint;
import com.mt.common.domain.model.restful.SumPagedRep;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = "application/json")
public class EndpointResource {

    @PostMapping(path = "projects/{projectId}/endpoints")
    public ResponseEntity<Void> tenantCreate(
        @PathVariable String projectId,
        @RequestBody EndpointCreateCommand command,
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        return ResponseEntity.ok().header("Location",
            ApplicationServiceRegistry.getEndpointApplicationService()
                .tenantCreate(projectId, command, changeId)).build();
    }

    @GetMapping(path = "projects/{projectId}/endpoints")
    public ResponseEntity<SumPagedRep<?>> tenantQuery(
        @PathVariable String projectId,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @RequestParam(value = HTTP_PARAM_QUERY, required = false) String queryParam,
        @RequestParam(value = HTTP_PARAM_PAGE, required = false) String pageParam,
        @RequestParam(value = HTTP_PARAM_SKIP_COUNT, required = false) String config
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        queryParam = updateProjectIds(queryParam, projectId);
        SumPagedRep<Endpoint> endpoints = ApplicationServiceRegistry.getEndpointApplicationService()
            .tenantQuery(queryParam, pageParam, config);
        SumPagedRep<EndpointCardRepresentation> rep =
            new SumPagedRep<>(endpoints, EndpointCardRepresentation::new);
        EndpointCardRepresentation.updateDetail(rep.getData());
        return ResponseEntity.ok(rep);
    }

    @GetMapping(path = "mgmt/endpoints")
    public ResponseEntity<SumPagedRep<?>> mgmtQuery(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @RequestParam(value = HTTP_PARAM_QUERY, required = false) String queryParam,
        @RequestParam(value = HTTP_PARAM_PAGE, required = false) String pageParam,
        @RequestParam(value = HTTP_PARAM_SKIP_COUNT, required = false) String config
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        SumPagedRep<Endpoint> endpoints = ApplicationServiceRegistry.getEndpointApplicationService()
            .mgmtQuery(queryParam, pageParam, config);
        SumPagedRep<EndpointCardRepresentation> endpointCardRepresentationSumPagedRep =
            new SumPagedRep<>(endpoints, EndpointCardRepresentation::new);
        EndpointCardRepresentation.updateDetail(endpointCardRepresentationSumPagedRep.getData());
        return ResponseEntity.ok(endpointCardRepresentationSumPagedRep);
    }

    @GetMapping(path = "mgmt/endpoints/{id}")
    public ResponseEntity<EndpointRepresentation> mgmtQuery(
        @PathVariable String id,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        Optional<Endpoint> endpoint =
            ApplicationServiceRegistry.getEndpointApplicationService().mgmtQuery(id);
        return endpoint.map(value -> ResponseEntity.ok(new EndpointRepresentation(value)))
            .orElseGet(() -> ResponseEntity.ok().build());
    }

    /**
     * get paginated endpoints for proxy to cache
     *
     * @param pageParam pagination info
     * @return paginated data
     */
    @GetMapping("endpoints/proxy")
    public ResponseEntity<SumPagedRep<?>> proxyQuery(
        @RequestParam(value = HTTP_PARAM_PAGE, required = false) String pageParam
    ) {
        SumPagedRep<EndpointProxyCacheRepresentation> endpoints =
            ApplicationServiceRegistry.getEndpointApplicationService()
                .proxyQuery(pageParam);
        return ResponseEntity.ok(endpoints);
    }

    @GetMapping("projects/{projectId}/endpoints/{id}")
    public ResponseEntity<EndpointRepresentation> tenantQuery(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @PathVariable String projectId,
        @PathVariable String id
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        Optional<Endpoint> endpoint = ApplicationServiceRegistry.getEndpointApplicationService()
            .tenantQuery(projectId, id);
        return endpoint.map(value -> ResponseEntity.ok(new EndpointRepresentation(value)))
            .orElseGet(() -> ResponseEntity.ok().build());
    }

    @PutMapping("projects/{projectId}/endpoints/{id}")
    public ResponseEntity<Void> tenantUpdate(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @PathVariable String projectId,
        @RequestBody EndpointUpdateCommand command,
        @PathVariable String id,
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        command.setProjectId(projectId);
        ApplicationServiceRegistry.getEndpointApplicationService().tenantUpdate(id, command, changeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("projects/{projectId}/endpoints/{id}")
    public ResponseEntity<Void> tenantRemove(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @PathVariable String projectId,
        @PathVariable String id,
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getEndpointApplicationService()
            .tenantRemove(projectId, id, changeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("projects/{projectId}/endpoints/{id}/expire")
    public ResponseEntity<Void> tenantExpire(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @PathVariable String projectId,
        @PathVariable String id,
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId,
        @RequestBody EndpointExpireCommand command
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getEndpointApplicationService()
            .expire(command, projectId, id, changeId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping(path = "projects/{projectId}/endpoints/{id}",
        consumes = "application/json-patch+json")
    public ResponseEntity<Void> tenantPatch(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @PathVariable String projectId,
        @PathVariable(name = "id") String id,
        @RequestBody JsonPatch patch,
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getEndpointApplicationService()
            .tenantPatch(projectId, id, patch, changeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "mgmt/endpoints/event/reload")
    public ResponseEntity<Void> mgmtReload(
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId
    ) {
        ApplicationServiceRegistry.getEndpointApplicationService().reloadCache(changeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "endpoints/shared")
    public ResponseEntity<SumPagedRep<EndpointSharedCardRepresentation>> marketQuery(
        @RequestParam(value = HTTP_PARAM_QUERY, required = false) String queryParam,
        @RequestParam(value = HTTP_PARAM_PAGE, required = false) String pageParam,
        @RequestParam(value = HTTP_PARAM_SKIP_COUNT, required = false) String config
    ) {
        SumPagedRep<Endpoint> shared = ApplicationServiceRegistry.getEndpointApplicationService()
            .marketQuery(queryParam, pageParam, config);
        SumPagedRep<EndpointSharedCardRepresentation> rep =
            new SumPagedRep<>(shared, EndpointSharedCardRepresentation::new);
        EndpointSharedCardRepresentation.updateDetail(rep.getData());
        return ResponseEntity.ok(rep);
    }
}
