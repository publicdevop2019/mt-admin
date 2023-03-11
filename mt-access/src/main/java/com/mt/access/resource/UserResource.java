package com.mt.access.resource;

import static com.mt.common.CommonConstant.HTTP_HEADER_AUTHORIZATION;
import static com.mt.common.CommonConstant.HTTP_HEADER_CHANGE_ID;
import static com.mt.common.CommonConstant.HTTP_PARAM_PAGE;
import static com.mt.common.CommonConstant.HTTP_PARAM_QUERY;
import static com.mt.common.CommonConstant.HTTP_PARAM_SKIP_COUNT;

import com.github.fge.jsonpatch.JsonPatch;
import com.mt.access.application.ApplicationServiceRegistry;
import com.mt.access.application.user.command.UpdateUserCommand;
import com.mt.access.application.user.command.UpdateUserRelationCommand;
import com.mt.access.application.user.command.UserCreateCommand;
import com.mt.access.application.user.command.UserForgetPasswordCommand;
import com.mt.access.application.user.command.UserResetPasswordCommand;
import com.mt.access.application.user.command.UserUpdateBizUserPasswordCommand;
import com.mt.access.application.user.command.UserUpdateProfileCommand;
import com.mt.access.application.user.representation.ProjectAdminRepresentation;
import com.mt.access.application.user.representation.UserCardRepresentation;
import com.mt.access.application.user.representation.UserMngmntRepresentation;
import com.mt.access.application.user.representation.UserProfileRepresentation;
import com.mt.access.application.user.representation.UserTenantRepresentation;
import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.image.Image;
import com.mt.access.domain.model.image.ImageId;
import com.mt.access.domain.model.user.User;
import com.mt.access.infrastructure.Utility;
import com.mt.common.domain.model.restful.PatchCommand;
import com.mt.common.domain.model.restful.SumPagedRep;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(produces = "application/json")
public class UserResource {
    public static final String CONTENT_TYPE = "content-type";
    public static final String LOCATION = "Location";

    /**
     * register new user.
     *
     * @param command  register command
     * @param changeId changeId
     * @return void
     */
    @PostMapping(path = "users")
    public ResponseEntity<Void> createForApp(@RequestBody UserCreateCommand command,
                                             @RequestHeader(HTTP_HEADER_CHANGE_ID)
                                             String changeId) {
        return ResponseEntity.ok().header("Location",
                ApplicationServiceRegistry.getUserApplicationService().create(command, changeId))
            .build();
    }

    @GetMapping(path = "mngmt/users")
    public ResponseEntity<SumPagedRep<UserCardRepresentation>> readForAdminByQuery(
        @RequestParam(value = HTTP_PARAM_QUERY, required = false) String queryParam,
        @RequestParam(value = HTTP_PARAM_PAGE, required = false) String pageParam,
        @RequestParam(value = HTTP_PARAM_SKIP_COUNT, required = false) String config) {
        SumPagedRep<User> users = ApplicationServiceRegistry.getUserApplicationService()
            .users(queryParam, pageParam, config);
        return ResponseEntity.ok(new SumPagedRep<>(users, UserCardRepresentation::new));
    }


    @GetMapping("mngmt/users/{id}")
    public ResponseEntity<UserMngmntRepresentation> readForAdminById(@PathVariable String id) {
        UserMngmntRepresentation detail =
            ApplicationServiceRegistry.getUserApplicationService().userDetailsForMngmnt(id);
        return ResponseEntity.ok(detail);
    }


    @PutMapping("mngmt/users/{id}")
    public ResponseEntity<Void> updateForAdmin(@RequestBody UpdateUserCommand command,
                                               @PathVariable String id,
                                               @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
                                               @RequestHeader(HTTP_HEADER_CHANGE_ID)
                                               String changeId) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getUserApplicationService().adminLock(id, command, changeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("mngmt/users/{id}")
    public ResponseEntity<Void> deleteForAdminById(@PathVariable String id,
                                                   @RequestHeader(HTTP_HEADER_CHANGE_ID)
                                                   String changeId,
                                                   @RequestHeader(HTTP_HEADER_AUTHORIZATION)
                                                   String jwt) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getUserApplicationService().delete(id, changeId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping(path = "mngmt/users/{id}", consumes = "application/json-patch+json")
    public ResponseEntity<Void> patchForAdminById(@PathVariable(name = "id") String id,
                                                  @RequestBody JsonPatch command,
                                                  @RequestHeader(HTTP_HEADER_CHANGE_ID)
                                                  String changeId,
                                                  @RequestHeader(HTTP_HEADER_AUTHORIZATION)
                                                  String jwt) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getUserApplicationService().patch(id, command, changeId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping(path = "mngmt/users")
    public ResponseEntity<Void> patchForAdminBatch(@RequestBody List<PatchCommand> patch,
                                                   @RequestHeader(HTTP_HEADER_CHANGE_ID)
                                                   String changeId) {
        ApplicationServiceRegistry.getUserApplicationService().patchBatch(patch, changeId);
        return ResponseEntity.ok().build();
    }

    @PutMapping(path = "users/pwd")
    public ResponseEntity<Void> updateForUser(@RequestBody UserUpdateBizUserPasswordCommand command,
                                              @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
                                              @RequestHeader(HTTP_HEADER_CHANGE_ID)
                                              String changeId) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getUserApplicationService().updatePassword(command, changeId);
        return ResponseEntity.ok().build();
    }

    /**
     * send forget pwd email to user email on system.
     *
     * @param command  forget pwd command
     * @param changeId change id
     * @return void
     */
    @PostMapping(path = "users/forgetPwd")
    public ResponseEntity<Void> forgetPwd(@RequestBody UserForgetPasswordCommand command,
                                          @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId) {
        ApplicationServiceRegistry.getUserApplicationService().forgetPassword(command, changeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "users/resetPwd")
    public ResponseEntity<Void> resetPwd(@RequestBody UserResetPasswordCommand command,
                                         @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId) {
        ApplicationServiceRegistry.getUserApplicationService().resetPassword(command, changeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "projects/{projectId}/users")
    public ResponseEntity<SumPagedRep<UserCardRepresentation>> findUsersForTenantProject(
        @PathVariable String projectId,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @RequestParam(value = HTTP_PARAM_QUERY, required = false) String queryParam,
        @RequestParam(value = HTTP_PARAM_PAGE, required = false) String pageParam,
        @RequestParam(value = HTTP_PARAM_SKIP_COUNT, required = false) String config
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        queryParam = Utility.updateProjectId(queryParam, projectId);
        SumPagedRep<User> users = ApplicationServiceRegistry.getUserRelationApplicationService()
            .tenantUsers(queryParam, pageParam, config);
        return ResponseEntity.ok(new SumPagedRep<>(users, UserCardRepresentation::new));
    }

    @GetMapping(path = "projects/{projectId}/users/{id}")
    public ResponseEntity<UserTenantRepresentation> findUserDetailForTenantProject(
        @PathVariable String projectId,
        @PathVariable String id,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        Optional<UserTenantRepresentation> user =
            ApplicationServiceRegistry.getUserRelationApplicationService()
                .getTenantUserDetail(projectId, id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.ok().build());
    }

    /**
     * read my profile.
     *
     * @param jwt user jwt
     * @return user profile
     */
    @GetMapping(path = "users/profile")
    public ResponseEntity<UserProfileRepresentation> getMyProfile(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        Optional<UserProfileRepresentation> user =
            ApplicationServiceRegistry.getUserApplicationService().myProfile();
        return user.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.ok().build());
    }

    /**
     * get my profile avatar.
     *
     * @param jwt user jwt
     * @return binary
     */
    @GetMapping(path = "users/profile/avatar")
    public ResponseEntity<byte[]> getMyProfileAvatar(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        Optional<Image> avatar =
            ApplicationServiceRegistry.getUserApplicationService().profileAvatar();
        return avatar.map(e -> {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set(CONTENT_TYPE,
                e.getContentType());
            responseHeaders.setContentDispositionFormData(e.getOriginalName(), e.getOriginalName());
            return ResponseEntity.ok().headers(responseHeaders).body(e.getSource());
        }).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    /**
     * update or create my profile avatar.
     *
     * @param jwt user jwt
     * @return void
     */
    @PostMapping(path = "users/profile/avatar")
    public ResponseEntity<Void> getMyProfileAvatar(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @RequestParam("file") MultipartFile file,
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ImageId imageId = ApplicationServiceRegistry.getUserApplicationService()
            .createProfileAvatar(file, changeId);
        return ResponseEntity.ok().header(LOCATION, imageId.getDomainId()).build();
    }

    /**
     * update my profile.
     *
     * @param jwt     user jwt
     * @param command update command
     * @return void
     */
    @PutMapping(path = "users/profile")
    public ResponseEntity<Void> findUserForProject3(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @RequestBody UserUpdateProfileCommand command
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getUserApplicationService().updateProfile(command);
        return ResponseEntity.ok().build();
    }

    /**
     * update user role for project.
     *
     * @param projectId project id
     * @param id        user id
     * @param jwt       jwt
     * @param command   update command
     * @return http response 200
     */
    @PutMapping(path = "projects/{projectId}/users/{id}")
    public ResponseEntity<Void> replaceUserDetailForProject(
        @PathVariable String projectId,
        @PathVariable String id,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId,
        @RequestBody UpdateUserRelationCommand command
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getUserRelationApplicationService()
            .update(projectId, id, command, changeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "projects/{projectId}/admins")
    public ResponseEntity<SumPagedRep<ProjectAdminRepresentation>> getAdminsForProject(
        @PathVariable String projectId,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @RequestParam(value = HTTP_PARAM_PAGE, required = false) String pageParam
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        SumPagedRep<ProjectAdminRepresentation> resp =
            ApplicationServiceRegistry.getUserRelationApplicationService()
                .adminsForProject(pageParam, projectId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping(path = "projects/{projectId}/admins/{userId}")
    public ResponseEntity<Void> addAdminsToProject(
        @PathVariable String projectId,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId,
        @PathVariable String userId
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getUserRelationApplicationService()
            .addAdmin(projectId, userId, changeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(path = "projects/{projectId}/admins/{userId}")
    public ResponseEntity<Void> removeAdminsToProject(
        @PathVariable String projectId,
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @RequestHeader(HTTP_HEADER_CHANGE_ID) String changeId,
        @PathVariable String userId
    ) {
        DomainRegistry.getCurrentUserService().setUser(jwt);
        ApplicationServiceRegistry.getUserRelationApplicationService()
            .removeAdmin(projectId, userId, changeId);
        return ResponseEntity.ok().build();
    }
}
