package com.mt.access.resource;

import static com.mt.common.CommonConstant.HTTP_HEADER_AUTHORIZATION;
import static com.mt.common.CommonConstant.HTTP_HEADER_CHANGE_ID;
import static com.mt.common.CommonConstant.HTTP_PARAM_PAGE;
import static com.mt.common.CommonConstant.HTTP_PARAM_SKIP_COUNT;

import com.mt.access.application.ApplicationServiceRegistry;
import com.mt.access.application.project.command.ProjectCreateCommand;
import com.mt.access.application.project.representation.DashboardRepresentation;
import com.mt.access.application.project.representation.ProjectCardRepresentation;
import com.mt.access.application.project.representation.ProjectRepresentation;
import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.project.Project;
import com.mt.common.domain.model.restful.SumPagedRep;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(produces = "application/json")
public class ProjectResource {

    @PostMapping(path = "projects")
    public ResponseEntity<Void> tenantCreate(
        @RequestBody ProjectCreateCommand command,
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt
    ) {
        DomainRegistry.getCurrentUserService().setUserJwt(jwt);
        return ResponseEntity.ok().header("Location",
                ApplicationServiceRegistry.getProjectApplicationService()
                    .tenantCreate(command, changeId))
            .build();
    }

    @GetMapping(path = "mgmt/projects")
    public ResponseEntity<SumPagedRep<ProjectCardRepresentation>> mgmtQuery(
        @RequestParam(value = HTTP_PARAM_PAGE, required = false) String pageParam,
        @RequestParam(value = HTTP_PARAM_SKIP_COUNT, required = false) String skipCount,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt
    ) {
        DomainRegistry.getCurrentUserService().setUserJwt(jwt);
        SumPagedRep<Project> queryProjects =
            ApplicationServiceRegistry.getProjectApplicationService()
                .mgmtQuery(pageParam, skipCount);
        SumPagedRep<ProjectCardRepresentation> projectCardRepresentationSumPagedRep =
            new SumPagedRep<>(queryProjects, ProjectCardRepresentation::new);
        ProjectCardRepresentation.updateCreatorName(projectCardRepresentationSumPagedRep);
        return ResponseEntity.ok(projectCardRepresentationSumPagedRep);
    }

    @GetMapping(path = "mgmt/dashboard")
    public ResponseEntity<DashboardRepresentation> mgmtQuery(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt
    ) {
        DomainRegistry.getCurrentUserService().setUserJwt(jwt);
        DashboardRepresentation rep =
            ApplicationServiceRegistry.getProjectApplicationService()
                .mgmtQuery();
        return ResponseEntity.ok(rep);
    }

    @GetMapping(path = "projects/tenant")
    public ResponseEntity<SumPagedRep<ProjectCardRepresentation>> tenantQuery(
        @RequestParam(value = HTTP_PARAM_PAGE, required = false) String pageParam,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt
    ) {
        DomainRegistry.getCurrentUserService().setUserJwt(jwt);
        SumPagedRep<Project> clients =
            ApplicationServiceRegistry.getProjectApplicationService().tenantQuery(pageParam);
        SumPagedRep<ProjectCardRepresentation> projectCardRepresentationSumPagedRep =
            new SumPagedRep<>(clients, ProjectCardRepresentation::new);
        return ResponseEntity.ok(projectCardRepresentationSumPagedRep);
    }

    @GetMapping("projects/{id}")
    public ResponseEntity<ProjectRepresentation> tenantQueryDetail(
        @PathVariable String id,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt
    ) {
        DomainRegistry.getCurrentUserService().setUserJwt(jwt);
        ProjectRepresentation resp =
            ApplicationServiceRegistry.getProjectApplicationService().tenantQueryDetail(id);
        return ResponseEntity.ok(resp);
    }

    /**
     * api to check if project created successfully.
     *
     * @param id  project id
     * @param jwt user jwt token
     * @return project creation status
     */
    @GetMapping("projects/{id}/ready")
    public ResponseEntity<Map<String, Boolean>> checkIfReady(
        @PathVariable String id,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt
    ) {
        DomainRegistry.getCurrentUserService().setUserJwt(jwt);
        boolean b =
            ApplicationServiceRegistry.getUserRelationApplicationService().checkExist(id);
        Map<String, Boolean> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("status", b);
        return ResponseEntity.ok(objectObjectHashMap);
    }
}
