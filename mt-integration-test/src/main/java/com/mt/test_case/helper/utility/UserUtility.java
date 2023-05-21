package com.mt.test_case.helper.utility;

import com.mt.test_case.helper.TenantContext;
import com.mt.test_case.helper.pojo.PendingUser;
import com.mt.test_case.helper.pojo.Project;
import com.mt.test_case.helper.pojo.Role;
import com.mt.test_case.helper.pojo.SumTotal;
import com.mt.test_case.helper.pojo.User;
import com.mt.test_case.helper.AppConstant;
import java.util.Objects;
import java.util.UUID;
import org.junit.Assert;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

public class UserUtility {
    private static final ParameterizedTypeReference<SumTotal<User>> reference =
        new ParameterizedTypeReference<>() {
        };

    private static String getUrl(Project project) {
        return UrlUtility.appendPath(TenantUtility.getTenantUrl(project), "users");
    }
//    public void initTestUser() {
//        if (testUser.size() == 0) {
//            log.debug("start of creating test users");
//            ResourceOwner resourceOwner1 = randomRegisterAnUser();
//            ResourceOwner resourceOwner2 = randomRegisterAnUser();
//            ResourceOwner resourceOwner3 = randomRegisterAnUser();
//            ResourceOwner resourceOwner4 = randomRegisterAnUser();
//            ResourceOwner resourceOwner5 = randomRegisterAnUser();
//            ResourceOwner resourceOwner6 = randomRegisterAnUser();
//            ResourceOwner resourceOwner7 = randomRegisterAnUser();
//            ResourceOwner resourceOwner8 = randomRegisterAnUser();
//            ResourceOwner resourceOwner9 = randomRegisterAnUser();
//            ResourceOwner resourceOwner10 = randomRegisterAnUser();
//            testUser.add(resourceOwner1);
//            testUser.add(resourceOwner2);
//            testUser.add(resourceOwner3);
//            testUser.add(resourceOwner4);
//            testUser.add(resourceOwner5);
//            testUser.add(resourceOwner6);
//            testUser.add(resourceOwner7);
//            testUser.add(resourceOwner8);
//            testUser.add(resourceOwner9);
//            testUser.add(resourceOwner10);
//            log.debug("end of creating test users");
//        } else {
//            log.debug("test users already exist");
//
//        }
//    }


    public static User createRandomUserObj() {
        return userCreateDraftObj(RandomUtility.randomStringWithNum() + "@gmail.com",
            RandomUtility.randomStringWithNum());
    }

    public static User userCreateDraftObj(String username, String password) {
        User user = new User();
        user.setEmail(username);
//        user.setPassword("P1!" + password.substring(0, 10));
        user.setPassword("Password1!");
        user.setMobileNumber("1231231234");
        user.setCountryCode("1");
        return user;
    }

    public static ResponseEntity<Void> register(User user) {
        ResponseEntity<DefaultOAuth2AccessToken> registerTokenResponse = OAuth2Utility
            .getOAuth2ClientCredentialToken(
                AppConstant.CLIENT_ID_REGISTER_ID, AppConstant.EMPTY_CLIENT_SECRET);
        createPendingUser(user);
        return enterActivationCode(user, registerTokenResponse.getBody().getValue());
    }

    public static ResponseEntity<Void> enterActivationCode(User user,
                                                           String registerToken) {
        PendingUser pendingUser = new PendingUser();
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setContentType(MediaType.APPLICATION_JSON);
        headers1.setBearerAuth(registerToken);
        headers1.set("changeId", UUID.randomUUID().toString());
        pendingUser.setEmail(user.getEmail());
        pendingUser.setCountryCode(user.getCountryCode());
        pendingUser.setMobileNumber(user.getMobileNumber());
        pendingUser.setPassword(user.getPassword());
        pendingUser.setActivationCode("123456");
        HttpEntity<PendingUser> request1 =
            new HttpEntity<>(pendingUser, headers1);
        return TestContext.getRestTemplate()
            .exchange(UrlUtility.getAccessUrl("/users"), HttpMethod.POST, request1,
                Void.class);
    }

    public static ResponseEntity<Void> createPendingUser(User user, String registerToken,
                                                         PendingUser pendingUser) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(registerToken);
        headers.set("changeId", UUID.randomUUID().toString());
        pendingUser.setEmail(user.getEmail());
        pendingUser.setMobileNumber(user.getMobileNumber());
        pendingUser.setCountryCode(user.getCountryCode());

        HttpEntity<PendingUser> request = new HttpEntity<>(pendingUser, headers);
        return TestContext.getRestTemplate()
            .exchange(UrlUtility.getAccessUrl("/pending-users"), HttpMethod.POST, request,
                Void.class);
    }

    public static ResponseEntity<Void> createPendingUser(User user) {
        ResponseEntity<DefaultOAuth2AccessToken> registerTokenResponse = OAuth2Utility
            .getOAuth2ClientCredentialToken(
                AppConstant.CLIENT_ID_REGISTER_ID, AppConstant.EMPTY_CLIENT_SECRET);
        PendingUser pendingUser = new PendingUser();
        return createPendingUser(user, registerTokenResponse.getBody().getValue(), pendingUser);
    }

    /**
     * register new user then login.
     *
     * @return login token
     */
    public static String registerNewUserThenLogin() {
        User randomUser = createRandomUserObj();
        register(randomUser);
        ResponseEntity<DefaultOAuth2AccessToken> loginTokenResponse =
            login(randomUser.getEmail(), randomUser.getPassword());
        return Objects.requireNonNull(loginTokenResponse.getBody()).getValue();
    }

    public static ResponseEntity<DefaultOAuth2AccessToken> login(String username,
                                                                 String userPwd) {
        return OAuth2Utility
            .getOAuth2PasswordToken(AppConstant.CLIENT_ID_LOGIN_ID, AppConstant.EMPTY_CLIENT_SECRET, username,
                userPwd);
    }

    public static String login(User user) {
        return OAuth2Utility
            .getOAuth2PasswordToken(AppConstant.CLIENT_ID_LOGIN_ID, AppConstant.EMPTY_CLIENT_SECRET, user.getEmail(),
                user.getPassword()).getBody().getValue();
    }

    public static ResponseEntity<DefaultOAuth2AccessToken> getJwtPasswordAdmin() {
        return login(AppConstant.ACCOUNT_USERNAME_ADMIN, AppConstant.ACCOUNT_PASSWORD_ADMIN);
    }

    public static ResponseEntity<DefaultOAuth2AccessToken> getJwtPasswordMallTenant() {
        return login(
            AppConstant.ACCOUNT_USERNAME_MALL_ADMIN, AppConstant.ACCOUNT_PASSWORD_MALL_ADMIN);
    }

    public static String getJwtUser() {
        return login(AppConstant.ACCOUNT_USERNAME_USER, AppConstant.ACCOUNT_PASSWORD_USER).getBody().getValue();
    }

    public static String getJwtAdmin() {
        return login(AppConstant.ACCOUNT_USERNAME_ADMIN, AppConstant.ACCOUNT_PASSWORD_ADMIN).getBody().getValue();
    }

    /**
     * create new user without any login to tenant project
     *
     * @return created user
     */
    public static User createUser() {
        User user = createRandomUserObj();
        ResponseEntity<Void> register = register(user);
        String s = UrlUtility.getId(register);
        user.setId(s);
        return user;
    }

    /**
     * create user whom login to tenant project
     *
     * @param project  tenant project
     * @param clientId sso client id
     * @return user logged in
     */
    public static User userLoginToTenant(Project project, String clientId) {
        //create new user then login to created project
        User tenantUser = createUser();
        String user1Token = login(tenantUser);
        ResponseEntity<String> codeResponse =
            OAuth2Utility.authorizeLogin(project.getId(), clientId, user1Token, AppConstant.TEST_REDIRECT_URL);
        Assert.assertEquals(HttpStatus.OK, codeResponse.getStatusCode());
        ResponseEntity<DefaultOAuth2AccessToken> oAuth2AuthorizationToken =
            OAuth2Utility.getOAuth2AuthorizationToken(
                OAuth2Utility.getAuthorizationCode(codeResponse),
                AppConstant.TEST_REDIRECT_URL, clientId, "");
        Assert.assertEquals(HttpStatus.OK, oAuth2AuthorizationToken.getStatusCode());
        return tenantUser;
    }

    public static ResponseEntity<Void> updateTenantUser(TenantContext tenantContext,
                                                        User user) {
        String url = getUrl(tenantContext.getProject());
        return Utility.updateResource(tenantContext.getCreator(), url, user,user.getId());
    }

    public static ResponseEntity<User> readTenantUser(TenantContext tenantContext,
                                                      User user) {
        String url = getUrl(tenantContext.getProject());
        return Utility.readResource(tenantContext.getCreator(), url, user.getId(), User.class);
    }

    public static ResponseEntity<SumTotal<User>> readTenantUsers(
        TenantContext tenantContext) {
        String url = getUrl(tenantContext.getProject());
        return Utility.readResource(tenantContext.getCreator(), url, reference);
    }

    public static ResponseEntity<SumTotal<User>> readTenantUsersByQuery(
        TenantContext tenantContext, String query) {
        String accessUrl = getUrl(tenantContext.getProject());
        String url = UrlUtility.appendQuery(accessUrl, query);
        return Utility.readResource(tenantContext.getCreator(), url, reference);
    }
}