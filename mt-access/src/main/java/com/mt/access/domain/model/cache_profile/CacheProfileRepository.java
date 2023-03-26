package com.mt.access.domain.model.cache_profile;

import com.mt.common.domain.model.restful.SumPagedRep;
import java.util.Optional;

public interface CacheProfileRepository {
    Optional<CacheProfile> id(CacheProfileId id);

    void add(CacheProfile cacheProfile);

    void remove(CacheProfile cacheProfile);

    SumPagedRep<CacheProfile> query(CacheProfileQuery cacheProfileQuery);
}
