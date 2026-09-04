package com.example.springbootapp.config;

import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_FUEL_PRICES_LIVE = "fuelPricesLive";
    public static final String CACHE_AI_ADVISOR = "aiAdvisor";
    public static final String CACHE_CATALOG_VARIANTS = "catalogVariants";
    public static final String CACHE_CATALOG_HIERARCHY = "catalogHierarchy";
    public static final String CACHE_CATALOG_BRANDS = "catalogBrands";
    public static final String CACHE_CATALOG_MODELS = "catalogModels";
    public static final String CACHE_IMMATRICULATION = "immatriculation";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(
                CACHE_FUEL_PRICES_LIVE,
                CACHE_AI_ADVISOR,
                CACHE_CATALOG_VARIANTS,
                CACHE_CATALOG_HIERARCHY,
                CACHE_CATALOG_BRANDS,
                CACHE_CATALOG_MODELS,
                CACHE_IMMATRICULATION
        ));
        return cacheManager;
    }
}
