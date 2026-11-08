package pro.zerobyte.animemizerbot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {
    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String ANIME_BY_ID = "animeById";
    public static final String ANIME_BY_TITLE = "animeByTitle";
    public static final String PROVIDER_FAILURES = "providerFailures";

    public enum CacheName {
        ANIME_BY_ID(CacheConfig.ANIME_BY_ID, Duration.ofHours(24), 1000),
        ANIME_BY_TITLE(CacheConfig.ANIME_BY_TITLE, Duration.ofHours(3), 500),
        PROVIDER_FAILURES(CacheConfig.PROVIDER_FAILURES, Duration.ofHours(24), 2000);

        private final String cacheName;
        private final Duration ttl;
        private final int maxSize;

        CacheName(String cacheName, Duration ttl, int maxSize) {
            this.cacheName = cacheName;
            this.ttl = ttl;
            this.maxSize = maxSize;
        }

        public String getCacheName() {
            return cacheName;
        }

        public CaffeineCache build() {
            return new CaffeineCache(cacheName,
                    Caffeine.newBuilder()
                            .expireAfterWrite(ttl)
                            .maximumSize(maxSize)
                            .build());
        }
    }

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.stream(CacheName.values())
                .map(CacheName::build)
                .toList());
        return manager;
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.warn("Cache GET error for cache '{}' key '{}': {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, Object value) {
                log.warn("Cache PUT error for cache '{}' key '{}': {}", cache.getName(), key, exception.getMessage());
            }
        };
    }
}
