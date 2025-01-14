package com.mt.access.domain.model.client;

import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.client.event.ClientAccessibilityRemoved;
import com.mt.access.domain.model.client.event.ClientAsResourceDeleted;
import com.mt.access.domain.model.client.event.ClientCreated;
import com.mt.access.domain.model.client.event.ClientDeleted;
import com.mt.access.domain.model.client.event.ClientGrantTypeChanged;
import com.mt.access.domain.model.client.event.ClientPathChanged;
import com.mt.access.domain.model.client.event.ClientResourcesChanged;
import com.mt.access.domain.model.client.event.ClientSecretChanged;
import com.mt.access.domain.model.client.event.ClientTokenDetailChanged;
import com.mt.access.domain.model.project.ProjectId;
import com.mt.access.domain.model.role.RoleId;
import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.audit.Auditable;
import com.mt.common.domain.model.exception.DefinedRuntimeException;
import com.mt.common.domain.model.exception.HttpResponseCode;
import com.mt.common.domain.model.local_transaction.TransactionContext;
import com.mt.common.domain.model.validate.Checker;
import com.mt.common.domain.model.validate.ValidationNotificationHandler;
import com.mt.common.domain.model.validate.Validator;
import com.mt.common.infrastructure.CommonUtility;
import com.mt.common.infrastructure.HttpValidationNotificationHandler;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Client extends Auditable {
    private static final String MT_ACCESS_ID = "0C8AZTODP4HT";
    private static final String MT_PROXY_ID = "0C8AZYTQ5W5C";
    private static final String MT_UI_REGISTER_ID_ = "0C8B00098WLD";
    private static final String MT_UI_LOGIN_ID = "0C8AZZ16LZB4";
    private static final Set<ClientId> reservedClientIds = new HashSet<>();
    private static final Pattern PATH_REGEX = Pattern.compile("^[a-z\\-/]*$");

    static {
        reservedClientIds.add(new ClientId(MT_ACCESS_ID));
        reservedClientIds.add(new ClientId(MT_PROXY_ID));
        reservedClientIds.add(new ClientId(MT_UI_REGISTER_ID_));
        reservedClientIds.add(new ClientId(MT_UI_LOGIN_ID));
    }

    @Getter
    private boolean isCreate = false;
    private Set<ClientId> resources = new LinkedHashSet<>();

    private Set<ClientId> externalResources = new LinkedHashSet<>();

    @Setter(AccessLevel.PRIVATE)
    @Getter
    private ProjectId projectId;

    @Getter
    private RoleId roleId;

    @Setter(AccessLevel.PRIVATE)
    @Getter
    private ClientId clientId;

    @Getter
    private String name;

    @Getter
    private String path;

    @Getter
    private ExternalUrl externalUrl;

    @Getter
    private String secret;

    @Getter
    private String description;

    private Set<ClientType> types = new LinkedHashSet<>();

    private Set<RedirectUrl> redirectUrls = new LinkedHashSet<>();

    @Getter
    private Boolean accessible;

    private Set<GrantType> grantTypes = new LinkedHashSet<>();

    @Getter
    private TokenDetail tokenDetail;
    private boolean resourceLoaded = false;
    private boolean externalResourceLoaded = false;
    private boolean grantTypeLoaded = false;
    private boolean typeLoaded = false;
    private boolean urlLoaded = false;

    public Client(ClientId clientId, ProjectId projectId, String name, String path,
                  @Nullable String secret, String description, Boolean accessible,
                  Set<ClientId> resources, Set<GrantType> grantTypes, TokenDetail tokenDetail,
                  Set<String> redirectUrls, Set<ClientType> types,
                  ExternalUrl externalUrl, TransactionContext context) {
        super();
        isCreate = true;
        setClientId(clientId);
        setProjectId(projectId);
        setResources(resources, context);
        setDescription(description);
        setAccessible(accessible, context);
        setName(name);
        setPath(path, context);
        initTypes(types);
        initSecret(secret);
        setGrantTypes(grantTypes, true, context);
        setTokenDetail(tokenDetail, context);
        setRedirectUrls(redirectUrls);
        setRoleId();
        setExternalUrl(externalUrl, context);
        //set id last so we know it's new object
        setId(CommonDomainRegistry.getUniqueIdGeneratorService().id());
        context.append(new ClientCreated(this));
        validate(new HttpValidationNotificationHandler());
        long milli = Instant.now().toEpochMilli();
        setCreatedAt(milli);
        setCreatedBy(DomainRegistry.getCurrentUserService().getUserId().getDomainId());
        setModifiedAt(milli);
        setModifiedBy(DomainRegistry.getCurrentUserService().getUserId().getDomainId());
    }

    public static Client fromDatabaseRow(Long id, Long createdAt, String createdBy, Long modifiedAt,
                                         String modifiedBy, Integer version,
                                         Boolean accessible, ClientId domainId,
                                         String description, String name, String path,
                                         ProjectId projectId, RoleId roleId, String secret,
                                         Integer accessTokenValiditySeconds,
                                         Integer refreshTokenValiditySeconds,
                                         ExternalUrl externalUrl) {
        Client client = new Client();
        client.setId(id);
        client.setCreatedAt(createdAt);
        client.setCreatedBy(createdBy);
        client.setModifiedAt(modifiedAt);
        client.setModifiedBy(modifiedBy);
        client.setVersion(version);
        client.accessible = accessible;
        client.setDescription(description);
        client.setName(name);
        client.path = path;
        client.setClientId(domainId);
        client.setProjectId(projectId);
        client.roleId = roleId;
        client.secret = secret;
        client.externalUrl = externalUrl;
        client.tokenDetail =
            new TokenDetail(accessTokenValiditySeconds, refreshTokenValiditySeconds);
        return client;
    }

    private void setExternalUrl(ExternalUrl externalUrl, TransactionContext context) {
        if (this.externalUrl == null && externalUrl == null) {
            return;
        } else if (this.externalUrl == null || externalUrl == null) {
            context.append(new ClientPathChanged(clientId));
        } else {
            if (!externalUrl.equals(this.externalUrl)) {
                context
                    .append(new ClientPathChanged(clientId));
            }
        }
        this.externalUrl = externalUrl;
    }

    private void setRoleId() {
        if (this.roleId != null) {
            throw new DefinedRuntimeException("client role cannot be overwritten", "1034",
                HttpResponseCode.BAD_REQUEST);
        }
        this.roleId = new RoleId();
    }

    private void initTypes(Set<ClientType> types) {
        Validator.notNull(types);
        Validator.notEmpty(types);
        if (Checker.notNullOrEmpty(this.types)) {
            throw new DefinedRuntimeException("client type can not be updated once created", "1035",
                HttpResponseCode.BAD_REQUEST);
        }
        if (types.stream().anyMatch(e -> e.equals(ClientType.FRONTEND_APP)) &&
            types.stream().anyMatch(e -> e.equals(ClientType.BACKEND_APP))) {
            throw new DefinedRuntimeException("client type conflict", "1036",
                HttpResponseCode.BAD_REQUEST);
        }
        this.types = types;
    }

    private void setPath(String path, TransactionContext context) {
        if (this.path == null && path == null) {
            return;
        } else if (this.path == null || path == null) {
            context.append(new ClientPathChanged(clientId));
        } else {
            if (!path.equals(this.path)) {
                context
                    .append(new ClientPathChanged(clientId));
            }
        }
        if (Checker.notNull(path)) {
            Validator.lessThanOrEqualTo(path, 50);
            Validator.greaterThanOrEqualTo(path, 5);
            Matcher matcher = PATH_REGEX.matcher(path);//alpha - / only
            boolean result = false;
            if (matcher.find()) {
                if (!path.startsWith("/") && !path.endsWith("/")) { //avoid /test/
                    if (!path.endsWith("-") && !path.startsWith("-")) { //avoid -test-
                        if (path.contains("/")) {
                            boolean valid = true;
                            for (String s : path.split("/")) {
                                if (s.startsWith("-") || s.endsWith("-")) {
                                    valid = false;
                                    break;
                                }
                                if (s.isBlank()) {
                                    valid = false;
                                    break;
                                }
                            }
                            if (valid) {
                                result = true;
                            }
                        } else {
                            result = true;
                        }
                    }
                }
            }
            if (!result) {
                throw new DefinedRuntimeException("invalid path format", "1084",
                    HttpResponseCode.BAD_REQUEST);
            }
        }
        this.path = path;
    }

    private void setGrantTypes(Set<GrantType> grantTypes, boolean isCreate,
                               TransactionContext context) {
        Validator.notNull(grantTypes);
        Validator.notEmpty(grantTypes);
        if (!isCreate) {

            if (!Checker.equals(grantTypes, this.grantTypes)) {
                context
                    .append(new ClientGrantTypeChanged(clientId));
            }
        }
        if (grantTypes.contains(GrantType.REFRESH_TOKEN) &&
            !grantTypes.contains(GrantType.PASSWORD)) {
            throw new DefinedRuntimeException("refresh token grant requires password grant", "1037",
                HttpResponseCode.BAD_REQUEST);
        }
        CommonUtility.updateCollection(this.grantTypes, grantTypes,
            () -> this.grantTypes = grantTypes);
    }

    private void setName(String name) {
        Validator.validRequiredString(1, 50, name);
        this.name = name.trim();
    }

    private void setDescription(String description) {
        Validator.validOptionalString(50, description);
        if (Checker.notNull(description)) {
            description = description.trim();
        }
        this.description = description;
    }

    private void setTokenDetail(TokenDetail tokenDetail, TransactionContext context) {
        if (id != null) {
            if (tokenDetailChanged(tokenDetail)) {
                context
                    .append(new ClientTokenDetailChanged(clientId));
            }
        }
        this.tokenDetail = tokenDetail;
    }

    private void setAccessible(Boolean accessible, TransactionContext context) {
        if (Checker.notNull(id)) {
            if (Checker.isTrue(getAccessible()) && Checker.isFalse(accessible)) {
                context
                    .append(new ClientAccessibilityRemoved(clientId));
            }
        }
        this.accessible = accessible;
    }

    private void setResources(Set<ClientId> resources, TransactionContext context) {
        if (Checker.notNull(resources)) {
            Validator.lessThanOrEqualTo(resources, 10);
        }
        if (Checker.notNull(id)) {
            if (resourcesChanged(resources)) {
                context
                    .append(new ClientResourcesChanged(clientId));
            }
        }
        Validator.notNull(resources);
        if (CommonUtility.collectionWillChange(this.resources, resources)) {
            CommonUtility.updateCollection(this.resources, resources,
                () -> this.resources = resources);
            DomainRegistry.getClientValidationService()
                .validate(this, new HttpValidationNotificationHandler());
        }
    }

    public Client replace(String name, String secret, String path, String description,
                          Boolean accessible, Set<ClientId> resources, Set<GrantType> grantTypes,
                          TokenDetail tokenDetail, Set<String> redirectUrl,
                          ExternalUrl externalUrl, TransactionContext context) {
        //load everything to avoid error
        getRedirectUrls();
        getGrantTypes();
        getTypes();
        getResources();
        getExternalResources();
        Client updated = CommonDomainRegistry.getCustomObjectSerializer().nativeDeepCopy(this);
        updated.setPath(path, context);
        updated.setResources(resources, context);
        updated.setAccessible(accessible, context);
        updated.updateSecret(secret, context);
        updated.setGrantTypes(grantTypes, false, context);
        updated.setTokenDetail(tokenDetail, context);
        updated.setName(name);
        updated.setDescription(description);
        updated.setRedirectUrls(redirectUrl);
        updated.setExternalUrl(externalUrl, context);
        updated.validate(new HttpValidationNotificationHandler());
        updated.setModifiedAt(Instant.now().toEpochMilli());
        updated.setModifiedBy(DomainRegistry.getCurrentUserService().getUserId().getDomainId());
        return updated;
    }

    @Override
    public void validate(ValidationNotificationHandler handler) {
        (new ClientValidator(this, handler)).validate();
    }

    //for create
    private void initSecret(String secret) {
        Validator.notNull(secret);
        Validator.notBlank(secret);
        this.secret = secret;
    }

    //for update
    private void updateSecret(String secret, TransactionContext context) {
        Validator.notNull(secret);
        Validator.notBlank(secret);
        if (!Checker.equals(secret, this.secret)) {
            context
                .append(new ClientSecretChanged(clientId));
            this.secret = secret;
        }
    }

    public Integer accessTokenValiditySeconds() {
        if (tokenDetail == null) {
            return null;
        }
        return tokenDetail.getAccessTokenValiditySeconds();
    }

    public Integer refreshTokenValiditySeconds() {
        if (tokenDetail == null) {
            return null;
        }
        return tokenDetail.getRefreshTokenValiditySeconds();
    }

    private boolean resourcesChanged(Set<ClientId> clientIds) {
        return !Checker.equals(this.resources, clientIds);
    }

    private boolean tokenDetailChanged(TokenDetail tokenDetail) {
        return !Checker.equals(this.tokenDetail, tokenDetail);
    }

    public void removeAllReferenced(TransactionContext context) {
        context.append(new ClientDeleted(clientId));
        if (Checker.isTrue(getAccessible())) {
            context
                .append(new ClientAsResourceDeleted(clientId));
        }
    }

    public boolean removable() {
        return !reservedClientIds.contains(this.clientId);
    }

    public void updateExternalResource(Set<ClientId> updated) {
        getExternalResources();
        if (CommonUtility.collectionWillChange(this.externalResources, updated)) {
            DomainRegistry.getClientRepository()
                .updateExternalResources(id, this.externalResources, updated);
            CommonUtility.updateCollection(this.externalResources, updated,
                () -> this.externalResources = updated);
            DomainRegistry.getClientValidationService()
                .validate(this, new HttpValidationNotificationHandler());
        }
    }

    public Set<ClientId> getResources() {
        if (isCreate) {
            return resources;
        }
        if (!resourceLoaded) {
            resources = DomainRegistry.getClientRepository().getResources(this.getId());
            resourceLoaded = true;
        }
        return resources;
    }

    public Set<ClientId> getExternalResources() {
        if (isCreate) {
            return externalResources;
        }
        if (!externalResourceLoaded) {
            externalResources =
                DomainRegistry.getClientRepository().getExternalResources(this.getId());
            externalResourceLoaded = true;
        }
        return externalResources;
    }

    public Set<GrantType> getGrantTypes() {
        if (isCreate) {
            return grantTypes;
        }
        if (!grantTypeLoaded) {
            grantTypes =
                DomainRegistry.getClientRepository().getGrantType(this.getId());
            grantTypeLoaded = true;
        }
        return grantTypes;
    }

    public Set<ClientType> getTypes() {
        if (isCreate) {
            return types;
        }
        if (!typeLoaded) {
            types =
                DomainRegistry.getClientRepository().getType(this.getId());
            typeLoaded = true;
        }
        return types;
    }

    public boolean sameAs(Client o) {
        return Objects.equals(getResources(), o.getResources()) &&
            Objects.equals(getExternalResources(), o.getExternalResources()) &&
            Objects.equals(projectId, o.projectId) &&
            Objects.equals(roleId, o.roleId) &&
            Objects.equals(clientId, o.clientId) &&
            Objects.equals(name, o.name) &&
            Objects.equals(path, o.path) &&
            Objects.equals(externalUrl, o.externalUrl) &&
            Objects.equals(secret, o.secret) &&
            Objects.equals(description, o.description) &&
            Objects.equals(getTypes(), o.getTypes()) &&
            Objects.equals(accessible, o.accessible) &&
            Objects.equals(redirectUrls, o.redirectUrls) &&
            Objects.equals(getGrantTypes(), o.getGrantTypes()) &&
            Objects.equals(tokenDetail, o.tokenDetail);
    }

    public int getRefreshTokenValiditySeconds() {
        if (grantTypes.contains(GrantType.PASSWORD)
            &&
            grantTypes.contains(GrantType.REFRESH_TOKEN)) {
            return getTokenDetail().getRefreshTokenValiditySeconds();
        }
        return 0;
    }

    public Set<String> getRegisteredRedirectUri() {
        return getRedirectUrls().stream().map(RedirectUrl::getValue)
            .collect(Collectors.toSet());

    }

    private void setRedirectUrls(Set<String> redirectUrls) {
        if (!Checker.isNullOrEmpty(redirectUrls)) {
            Validator.lessThanOrEqualTo(redirectUrls, 5);
            Set<RedirectUrl> collect =
                redirectUrls.stream().map(RedirectUrl::new).collect(Collectors.toSet());
            CommonUtility.updateCollection(this.redirectUrls, collect,
                () -> this.redirectUrls = collect);
        }
        //set to true so url will not be loaded again
        urlLoaded = true;
    }

    public Set<RedirectUrl> getRedirectUrls() {
        if (isCreate()) {
            return redirectUrls;
        }
        if (!urlLoaded) {
            redirectUrls =
                DomainRegistry.getClientRepository().getRedirectUrls(id);
            urlLoaded = true;
        }
        return redirectUrls;
    }
}
