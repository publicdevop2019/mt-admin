package com.mt.test_case.integration.single.access.mgmt;

import com.mt.test_case.helper.AppConstant;
import com.mt.test_case.helper.pojo.SumTotal;
import com.mt.test_case.helper.pojo.User;
import com.mt.test_case.helper.pojo.UserMgmt;
import com.mt.test_case.helper.utility.RandomUtility;
import com.mt.test_case.helper.utility.TestContext;
import com.mt.test_case.helper.utility.UrlUtility;
import com.mt.test_case.helper.utility.UserUtility;
import com.mt.test_case.helper.CommonTest;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Slf4j
public class MgmtUserTest extends CommonTest {
    public static final String USER_MGMT = "/mgmt/users";
    private static final String root_index = "0U8AZTODP4H0";

    @Test
    public void admin_can_view_all_users() {
        String url = UrlUtility.getAccessUrl(USER_MGMT);
        String jwtAdmin = UserUtility.getJwtAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtAdmin);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<SumTotal<User>> exchange = TestContext.getRestTemplate()
            .exchange(url, HttpMethod.GET, request, new ParameterizedTypeReference<>() {
            });

        Assert.assertNotSame(0, Objects.requireNonNull(exchange.getBody()).getData().size());
    }

    @Test
    public void admin_can_view_user_detail_include_login_history() {
        //include login history
        String jwtAdmin = UserUtility.getJwtAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtAdmin);
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<SumTotal<User>> exchange = TestContext.getRestTemplate()
            .exchange(UrlUtility.getAccessUrl(USER_MGMT), HttpMethod.GET, request,
                new ParameterizedTypeReference<>() {
                });
        List<User> data = Objects.requireNonNull(exchange.getBody()).getData();
        int i = RandomUtility.pickRandomFromList(data.size());
        User user = data.get(i);
        ResponseEntity<UserMgmt> exchange2 = TestContext.getRestTemplate()
            .exchange(UrlUtility.getAccessUrl(UrlUtility.combinePath(USER_MGMT, user.getId())),
                HttpMethod.GET, request, UserMgmt.class);
        Assert.assertEquals(HttpStatus.OK, exchange2.getStatusCode());
        Assert.assertNotNull(Objects.requireNonNull(exchange2.getBody()).getLoginHistory());
    }

    @Test
    public void admin_can_delete_user() {
        User user = UserUtility.createRandomUserObj();
        ResponseEntity<Void> user1 = UserUtility.register(user);

        String url = UrlUtility.getAccessUrl(USER_MGMT + "/" + UrlUtility.getId(user1));

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse12 =
            UserUtility.login(user.getEmail(), user.getPassword());

        Assert.assertEquals(HttpStatus.OK, tokenResponse12.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = UserUtility.login(
            AppConstant.ACCOUNT_USERNAME_ADMIN, AppConstant.ACCOUNT_PASSWORD_ADMIN);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(Objects.requireNonNull(tokenResponse.getBody()).getValue());
        HttpEntity<Object> request = new HttpEntity<>(null, headers);
        ResponseEntity<Object> exchange =
            TestContext.getRestTemplate().exchange(url, HttpMethod.DELETE, request, Object.class);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse123 =
            UserUtility.login(user.getEmail(), user.getPassword());

        Assert.assertEquals(HttpStatus.BAD_REQUEST, tokenResponse123.getStatusCode());

    }

    @Test
    public void admin_cannot_delete_root_user() {

        String url =
            UrlUtility.getAccessUrl(USER_MGMT + "/" + root_index);

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse12 = UserUtility.login(
            AppConstant.ACCOUNT_USERNAME_ADMIN, AppConstant.ACCOUNT_PASSWORD_ADMIN);

        Assert.assertEquals(HttpStatus.OK, tokenResponse12.getStatusCode());
        //try w root
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = UserUtility.login(
            AppConstant.ACCOUNT_USERNAME_ADMIN, AppConstant.ACCOUNT_PASSWORD_ADMIN);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(Objects.requireNonNull(tokenResponse.getBody()).getValue());
        HttpEntity<Object> request = new HttpEntity<>(null, headers);
        ResponseEntity<Object> exchange =
            TestContext.getRestTemplate().exchange(url, HttpMethod.DELETE, request, Object.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, exchange.getStatusCode());

    }

    @Test
    public void admin_can_lock_then_unlock_user() {
        User user = UserUtility.createRandomUserObj();
        ResponseEntity<Void> createResp = UserUtility.register(user);

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = UserUtility.login(
            AppConstant.ACCOUNT_USERNAME_ADMIN, AppConstant.ACCOUNT_PASSWORD_ADMIN);
        String bearer = Objects.requireNonNull(tokenResponse.getBody()).getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        user.setLocked(true);
        user.setVersion(0);
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        String url = UrlUtility.getAccessUrl(USER_MGMT + "/" + UrlUtility.getId(createResp));
        ResponseEntity<DefaultOAuth2AccessToken> exchange = TestContext.getRestTemplate()
            .exchange(url, HttpMethod.PUT, request, DefaultOAuth2AccessToken.class);

        Assert.assertEquals(HttpStatus.OK, exchange.getStatusCode());
        //login to verify account has been locked
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse1 =
            UserUtility.login(user.getEmail(), user.getPassword());
        Assert.assertEquals(HttpStatus.BAD_REQUEST, tokenResponse1.getStatusCode());

        user.setLocked(false);
        user.setVersion(1);
        HttpEntity<User> request22 = new HttpEntity<>(user, headers);
        ResponseEntity<DefaultOAuth2AccessToken> exchange22 = TestContext.getRestTemplate()
            .exchange(url, HttpMethod.PUT, request22, DefaultOAuth2AccessToken.class);

        Assert.assertEquals(HttpStatus.OK, exchange22.getStatusCode());
        //login to verify account has been unlocked
        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse12 =
            UserUtility.login(user.getEmail(), user.getPassword());
        Assert.assertEquals(HttpStatus.OK, tokenResponse12.getStatusCode());
    }


    @Test
    public void user_cannot_update_user_via_mgmt() {
        User user = UserUtility.createRandomUserObj();

        ResponseEntity<DefaultOAuth2AccessToken> tokenResponse = UserUtility.login(
            AppConstant.ACCOUNT_USERNAME_USER, AppConstant.ACCOUNT_PASSWORD_USER);
        String bearer = Objects.requireNonNull(tokenResponse.getBody()).getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        String url =
            UrlUtility.getAccessUrl(USER_MGMT + "/" + root_index);
        ResponseEntity<Void> exchange = TestContext.getRestTemplate()
            .exchange(url, HttpMethod.PUT, request, Void.class);
        Assert.assertEquals(HttpStatus.FORBIDDEN, exchange.getStatusCode());

    }

}