package com.mt.access.port.adapter.persistence.endpoint;

import com.mt.access.domain.model.cache_profile.CacheProfileId;
import com.mt.access.domain.model.client.ClientId;
import com.mt.access.domain.model.cors_profile.CorsProfileId;
import com.mt.access.domain.model.endpoint.Endpoint;
import com.mt.access.domain.model.endpoint.EndpointId;
import com.mt.access.domain.model.endpoint.EndpointQuery;
import com.mt.access.domain.model.endpoint.EndpointRepository;
import com.mt.access.domain.model.endpoint.Endpoint_;
import com.mt.access.domain.model.project.ProjectId;
import com.mt.access.port.adapter.persistence.QueryBuilderRegistry;
import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.domain_event.DomainId;
import com.mt.common.domain.model.restful.SumPagedRep;
import com.mt.common.domain.model.restful.query.QueryUtility;
import com.mt.common.domain.model.validate.Checker;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataJpaEndpointRepository
    extends JpaRepository<Endpoint, Long>, EndpointRepository {

    default Endpoint query(EndpointId endpointId) {
        return query(new EndpointQuery(endpointId)).findFirst().orElse(null);
    }

    default Set<CacheProfileId> getCacheProfileIds() {
        return getCacheProfileIds_();
    }

    default Set<CorsProfileId> getCorsProfileIds() {
        return getCorsProfileIds_();
    }

    default Set<ClientId> getClientIds() {
        return getClientIds_();
    }

    @Query("select distinct ep.cacheProfileId from Endpoint ep where ep.cacheProfileId is not null")
    Set<CacheProfileId> getCacheProfileIds_();

    @Query("select distinct ep.corsProfileId from Endpoint ep where ep.corsProfileId is not null")
    Set<CorsProfileId> getCorsProfileIds_();

    @Query("select distinct ep.clientId from Endpoint ep")
    Set<ClientId> getClientIds_();

    @Query("select count(*) from Endpoint")
    Long countTotal_();

    @Query("select count(*) from Endpoint ep where ep.shared = true")
    Long countSharedTotal_();

    @Query("select count(*) from Endpoint ep where ep.secured = false and ep.external = true")
    Long countPublicTotal_();

    @Query("select count(*) from Endpoint ep where ep.projectId = ?1")
    Long countProjectTotal_(ProjectId projectId);

    default void add(Endpoint endpoint) {
        save(endpoint);
    }

    default void remove(Endpoint endpoint) {
        delete(endpoint);
    }

    default void remove(Collection<Endpoint> endpoints) {
        deleteAll(endpoints);
    }

    default SumPagedRep<Endpoint> query(EndpointQuery query) {
        return QueryBuilderRegistry.getEndpointQueryBuilder().execute(query);
    }


    default long countTotal() {
        return countTotal_();
    }

    default long countSharedTotal() {
        return countSharedTotal_();
    }

    default long countPublicTotal() {
        return countPublicTotal_();
    }

    default long countProjectTotal(ProjectId projectId) {
        return countProjectTotal_(projectId);
    }

    default boolean checkDuplicate(ClientId clientId, String path, String method) {
        Object query = CommonDomainRegistry.getJdbcTemplate()
            .query(
                "SELECT COUNT(*) AS count FROM endpoint e WHERE e.client_id = ? AND e.path = ? AND e.method = ?",
                new Object[] {
                    clientId.getDomainId(),
                    path,
                    method
                },
                new ResultSetExtractor<Object>() {
                    @Override
                    public Object extractData(ResultSet rs)
                        throws SQLException, DataAccessException {
                        if (!rs.next()) {
                            return null;
                        }
                        return rs.getInt("count");
                    }
                });
        return ((Integer) query) > 0;
    }

    @Component
    class JpaCriteriaApiEndpointAdapter {
        public SumPagedRep<Endpoint> execute(EndpointQuery endpointQuery) {
            QueryUtility.QueryContext<Endpoint> queryContext =
                QueryUtility.prepareContext(Endpoint.class, endpointQuery);
            Optional.ofNullable(endpointQuery.getEndpointIds()).ifPresent(e -> QueryUtility
                .addDomainIdInPredicate(
                    e.stream().map(DomainId::getDomainId).collect(Collectors.toSet()),
                    Endpoint_.ENDPOINT_ID, queryContext));
            Optional.ofNullable(endpointQuery.getClientIds()).ifPresent(e -> QueryUtility
                .addDomainIdInPredicate(
                    e.stream().map(DomainId::getDomainId).collect(Collectors.toSet()),
                    Endpoint_.CLIENT_ID, queryContext));
            Optional.ofNullable(endpointQuery.getProjectIds()).ifPresent(e -> QueryUtility
                .addDomainIdInPredicate(
                    e.stream().map(DomainId::getDomainId).collect(Collectors.toSet()),
                    Endpoint_.PROJECT_ID, queryContext));
            Optional.ofNullable(endpointQuery.getPermissionIds()).ifPresent(e -> QueryUtility
                .addDomainIdInPredicate(
                    e.stream().map(DomainId::getDomainId).collect(Collectors.toSet()),
                    Endpoint_.PERMISSION_ID, queryContext));
            Optional.ofNullable(endpointQuery.getCacheProfileIds()).ifPresent(e -> QueryUtility
                .addDomainIdInPredicate(
                    e.stream().map(DomainId::getDomainId).collect(Collectors.toSet()),
                    Endpoint_.CACHE_PROFILE_ID, queryContext));
            Optional.ofNullable(endpointQuery.getPath()).ifPresent(
                e -> QueryUtility.addStringEqualPredicate(e, Endpoint_.PATH, queryContext));
            Optional.ofNullable(endpointQuery.getMethod()).ifPresent(
                e -> QueryUtility.addStringEqualPredicate(e, Endpoint_.METHOD, queryContext));
            Optional.ofNullable(endpointQuery.getIsWebsocket()).ifPresent(e -> QueryUtility
                .addBooleanEqualPredicate(e, Endpoint_.WEBSOCKET, queryContext));
            Optional.ofNullable(endpointQuery.getIsShared()).ifPresent(
                e -> {
                    if (e) {
                        queryContext.getPredicates().add(
                            MarketAvailableEndpointPredicateConverter
                                .getPredicate(queryContext.getCriteriaBuilder(),
                                    queryContext.getRoot()));
                        Optional.ofNullable(queryContext.getCountPredicates())
                            .ifPresent(ee -> ee.add(
                                MarketAvailableEndpointPredicateConverter
                                    .getPredicate(queryContext.getCriteriaBuilder(),
                                        queryContext.getCountRoot())));
                    }
                }
            );

            Optional.ofNullable(endpointQuery.getIsSecured()).ifPresent(
                e -> QueryUtility
                    .addBooleanEqualPredicate(e, Endpoint_.SECURED, queryContext));
            Optional.ofNullable(endpointQuery.getCorsProfileIds()).ifPresent(e -> QueryUtility
                .addDomainIdInPredicate(
                    e.stream().map(DomainId::getDomainId).collect(Collectors.toSet()),
                    Endpoint_.CORS_PROFILE_ID, queryContext));
            Order order = null;
            if (Checker.isTrue(endpointQuery.getEndpointSort().getById())) {
                order = QueryUtility.getDomainIdOrder(Endpoint_.ENDPOINT_ID, queryContext,
                    endpointQuery.getEndpointSort().getIsAsc());
            }
            if (Checker.isTrue(endpointQuery.getEndpointSort().getByClientId())) {
                order = QueryUtility.getOrder(Endpoint_.CLIENT_ID, queryContext,
                    endpointQuery.getEndpointSort().getIsAsc());
            }
            if (Checker.isTrue(endpointQuery.getEndpointSort().getByPath())) {
                order = QueryUtility.getOrder(Endpoint_.PATH, queryContext,
                    endpointQuery.getEndpointSort().getIsAsc());
            }
            if (Checker.isTrue(endpointQuery.getEndpointSort().getByMethod())) {
                order = QueryUtility.getOrder(Endpoint_.METHOD, queryContext,
                    endpointQuery.getEndpointSort().getIsAsc());
            }
            queryContext.setOrder(order);
            return QueryUtility.nativePagedQuery(endpointQuery, queryContext);
        }

        private static class MarketAvailableEndpointPredicateConverter {
            public static Predicate getPredicate(CriteriaBuilder cb,
                                                 Root<Endpoint> root) {
                Predicate isShared = cb.isTrue(root.get(Endpoint_.SHARED));
                Predicate noAuth = cb.isFalse(root.get(Endpoint_.SECURED));
                Predicate isExternal = cb.isTrue(root.get(Endpoint_.EXTERNAL));
                Predicate and = cb.and(isExternal, noAuth);
                return cb.or(isShared, and);
            }
        }
    }
}
