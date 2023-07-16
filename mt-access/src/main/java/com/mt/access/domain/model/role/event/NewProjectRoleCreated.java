package com.mt.access.domain.model.role.event;

import com.mt.access.domain.model.permission.PermissionId;
import com.mt.access.domain.model.project.ProjectId;
import com.mt.access.domain.model.role.RoleId;
import com.mt.access.domain.model.user.UserId;
import com.mt.common.domain.model.domain_event.DomainEvent;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NewProjectRoleCreated extends DomainEvent {
    public static final String NEW_PROJECT_ROLE_CREATED = "new_project_role_created";
    public static final String name = "NEW_PROJECT_ROLE_CREATED";
    @Getter
    private UserId creator;
    @Getter
    private ProjectId projectId;
    @Getter
    private RoleId userRoleId;

    {
        setTopic(NEW_PROJECT_ROLE_CREATED);
        setName(name);
    }

    public NewProjectRoleCreated(RoleId adminRoleId, RoleId userRoleId, ProjectId projectId,
                                 UserId creator) {
        super(adminRoleId);
        this.creator = creator;
        this.userRoleId = userRoleId;
        this.projectId = projectId;
    }
}
