package com.demo.skills.cache;

import java.time.Duration;
import lombok.val;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

/** Redis cache configuration. Serialises values as JSON and sets per-cache TTLs. */
@Configuration
@EnableCaching
public class CacheConfig {

  private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

  /**
   * Creates the Redis cache manager with JSON serialisation and explicit TTLs.
   *
   * <p>Uses the Spring Boot {@link ObjectMapper} (with JavaTimeModule registered) so that
   * {@code OffsetDateTime} and other temporal types serialise correctly.
   */
  @Bean
  RedisCacheManager redisCacheManager(
      final RedisConnectionFactory redisConnectionFactory, final ObjectMapper objectMapper) {
    val valueSerializer =
        RedisSerializationContext.SerializationPair.fromSerializer(
            new GenericJacksonJsonRedisSerializer(objectMapper));
    val keySerializer =
        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());

    val config =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(DEFAULT_TTL)
            .serializeKeysWith(keySerializer)
            .serializeValuesWith(valueSerializer);

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(config)
        .withCacheConfiguration("accounts", config)
        .withCacheConfiguration("accountList", config)
        .build();
  }
}
