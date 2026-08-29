package com.masasilam.app.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    public static final String MONOGRAPH_DETAIL_CACHE = "monographDetail";
    public static final String CHAPTER_BY_PATH_CACHE = "chapter-by-path";
    public static final String CHAPTERS_LIST_CACHE = "chapters-list";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                MONOGRAPH_DETAIL_CACHE,
                CHAPTER_BY_PATH_CACHE,
                CHAPTERS_LIST_CACHE
        );
    }
}