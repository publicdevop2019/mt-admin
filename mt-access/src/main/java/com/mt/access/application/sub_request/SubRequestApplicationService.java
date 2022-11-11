package com.mt.access.application.sub_request;

import static com.mt.access.domain.model.permission.Permission.SUB_REQ_MNGMT;

import com.mt.access.application.sub_request.command.CreateSubRequestCommand;
import com.mt.access.application.sub_request.command.RejectSubRequestCommand;
import com.mt.access.application.sub_request.command.UpdateSubRequestCommand;
import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.audit.AuditLog;
import com.mt.access.domain.model.endpoint.Endpoint;
import com.mt.access.domain.model.endpoint.EndpointId;
import com.mt.access.domain.model.project.ProjectId;
import com.mt.access.domain.model.sub_request.SubRequest;
import com.mt.access.domain.model.sub_request.SubRequestId;
import com.mt.access.domain.model.sub_request.SubRequestQuery;
import com.mt.access.domain.model.user.UserId;
import com.mt.common.application.CommonApplicationServiceRegistry;
import com.mt.common.domain.model.restful.SumPagedRep;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubRequestApplicationService {

    public static final String SUB_REQUEST = "SUB_REQUEST";

    public SumPagedRep<SubRequest> query(String queryParam, String pageParam) {
        Set<ProjectId> tenantIds = DomainRegistry.getCurrentUserService().getTenantIds();
        DomainRegistry.getPermissionCheckService()
            .canAccess(tenantIds, SUB_REQ_MNGMT);
        return DomainRegistry.getSubRequestRepository()
            .getByQuery(new SubRequestQuery(queryParam, pageParam));
    }

    /**
     * create sub request
     *
     * @param command  create command
     * @param changeId unique change id
     * @return created sub request domain id
     */
    @Transactional
    @AuditLog(actionName = "create sub request")
    public String create(CreateSubRequestCommand command, String changeId) {
        return CommonApplicationServiceRegistry.getIdempotentService()
            .idempotent(changeId, (ignored) -> {
                EndpointId endpointId = new EndpointId(command.getEndpointId());
                Optional<Endpoint> endpoint =
                    DomainRegistry.getEndpointRepository().endpointOfId(endpointId);
                if (endpoint.isEmpty()) {
                    throw new IllegalArgumentException("unable to find related endpoint");
                }
                ProjectId epProjectId = endpoint.get().getProjectId();
                ProjectId targetProjectId = new ProjectId(command.getProjectId());
                if (epProjectId.equals(targetProjectId)) {
                    throw new IllegalArgumentException("cannot subscribe to itself");
                }
                SubRequest subRequest = new SubRequest(
                    new ProjectId(command.getProjectId()),
                    endpointId,
                    command.getMaxInvokePerSec(),
                    command.getMaxInvokePerMin(),
                    epProjectId
                );
                DomainRegistry.getSubRequestRepository().add(subRequest);
                return subRequest.getSubRequestId().getDomainId();
            }, SUB_REQUEST);
    }

    /**
     * update sub request.
     *
     * @param id       sub request id
     * @param command  update command
     * @param changeId unique change id
     */
    @Transactional
    public void update(String id, UpdateSubRequestCommand command, String changeId) {
        Optional<SubRequest> byId =
            DomainRegistry.getSubRequestRepository().getById(new SubRequestId(id));
        byId.ifPresent(ee -> {
            DomainRegistry.getPermissionCheckService().sameCreatedBy(ee);
            CommonApplicationServiceRegistry.getIdempotentService()
                .idempotent(changeId, (ignored) -> {
                    ee.update(command.getMaxInvokePerSec(), command.getMaxInvokePerMin());
                    return null;
                }, SUB_REQUEST);
        });
    }

    /**
     * cancel sub request.
     *
     * @param id       sub request id
     * @param changeId unique change id
     */
    @Transactional
    public void cancel(String id, String changeId) {
        SubRequestId subRequestId = new SubRequestId(id);
        Optional<SubRequest> byId = DomainRegistry.getSubRequestRepository().getById(subRequestId);
        byId.ifPresent(e -> {
            DomainRegistry.getPermissionCheckService().sameCreatedBy(e);
            CommonApplicationServiceRegistry.getIdempotentService()
                .idempotent(changeId, (ignored) -> {
                    byId.ifPresent(
                        SubRequest::cancel);
                    return null;
                }, SUB_REQUEST);
        });
    }

    /**
     * approve sub request.
     *
     * @param id       sub request id
     * @param changeId unique change id
     */
    @Transactional
    @AuditLog(actionName = "approve sub request")
    public void approve(String id, String changeId) {
        SubRequestId subRequestId = new SubRequestId(id);
        Optional<SubRequest> byId = DomainRegistry.getSubRequestRepository().getById(subRequestId);
        byId.ifPresent(e -> {
            ProjectId endpointProjectId = e.getEndpointProjectId();
            DomainRegistry.getPermissionCheckService()
                .canAccess(endpointProjectId, SUB_REQ_MNGMT);
            CommonApplicationServiceRegistry.getIdempotentService()
                .idempotent(changeId, (ignored) -> {
                    UserId userId = DomainRegistry.getCurrentUserService().getUserId();
                    byId.ifPresent(e1 -> e1.approve(userId));
                    return null;
                }, SUB_REQUEST);
        });
    }

    /**
     * reject sub request.
     *
     * @param id       sub request id
     * @param command  reject command
     * @param changeId unique change id
     */
    @Transactional
    @AuditLog(actionName = "reject sub request")
    public void reject(String id, RejectSubRequestCommand command, String changeId) {
        SubRequestId subRequestId = new SubRequestId(id);
        Optional<SubRequest> byId = DomainRegistry.getSubRequestRepository().getById(subRequestId);
        byId.ifPresent(e -> {
            ProjectId endpointProjectId = e.getEndpointProjectId();
            DomainRegistry.getPermissionCheckService()
                .canAccess(endpointProjectId, SUB_REQ_MNGMT);
            CommonApplicationServiceRegistry.getIdempotentService()
                .idempotent(changeId, (ignored) -> {
                    UserId userId = DomainRegistry.getCurrentUserService().getUserId();
                    byId.ifPresent(reject -> reject.reject(command.getRejectionReason(), userId));
                    return null;
                }, SUB_REQUEST);
        });
    }
}