package com.example.springbootapp.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

class CacheConfigTest {

    @Test
    void shouldInitializeCacheManagerWithConfiguredCaches() {
        CacheConfig config = new CacheConfig();
        CacheManager cacheManager = config.cacheManager();

        assertNotNull(cacheManager);
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_FUEL_PRICES_LIVE));
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_AI_ADVISOR));
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_CATALOG_VARIANTS));
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_CATALOG_HIERARCHY));
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_CATALOG_BRANDS));
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_CATALOG_MODELS));
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_IMMATRICULATION));
    }
}
