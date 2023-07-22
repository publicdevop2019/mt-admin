package com.mt.integration.single.access.validation;

import com.mt.helper.TenantTest;
import com.mt.helper.args.UserRoleArgs;
import com.mt.helper.pojo.User;
import com.mt.helper.utility.UserUtility;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
@Tag("validation")

@ExtendWith(SpringExtension.class)
@Slf4j
public class TenantUserValidationTest extends TenantTest {

    @ParameterizedTest
    @ArgumentsSource(UserRoleArgs.class)
    public void validation_update_user_role_ids(List<String> roles, HttpStatus httpStatus) {
        User user = tenantContext.getUsers().get(0);
        ResponseEntity<User> userResponseEntity = UserUtility.readTenantUser(tenantContext, user);
        User body = userResponseEntity.getBody();
        Objects.requireNonNull(body).setRoles(roles);
        ResponseEntity<Void> response = UserUtility.updateTenantUser(tenantContext, body);
        Assertions.assertEquals(httpStatus, response.getStatusCode());
    }
}
