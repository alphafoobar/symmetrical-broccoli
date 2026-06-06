package com.demo.skills.cache;

import com.demo.skills.api.model.AccountListResponse;
import com.demo.skills.api.model.AccountResponse;
import java.time.Duration;
import java.util.List;
import lombok.val;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/** Redis cache configuration. Serializes values as JSON and sets per-cache TTLs. */
@Configuration
@EnableCaching
public class CacheConfig {

  private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

  /**
   * Creates the default Redis cache configuration with JSON value serialization.
   *
   * <p>Uses the Spring Boot {@link ObjectMapper} (with JavaTimeModule registered) so that
   * {@code OffsetDateTime} and other temporal types serialize correctly.
   */
  @Bean
  RedisCacheConfiguration redisCacheConfiguration(final ObjectMapper objectMapper) {
    val valueSerializer =
        RedisSerializationContext.SerializationPair.fromSerializer(
            new GenericJacksonJsonRedisSerializer(objectMapper));
    val keySerializer =
        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());

    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(DEFAULT_TTL)
        .serializeKeysWith(keySerializer)
        .serializeValuesWith(valueSerializer);
  }

  /** Creates the Redis cache manager with explicit cache configurations. */
  @Bean
  RedisCacheManager redisCacheManager(
      final RedisConnectionFactory redisConnectionFactory,
      final RedisCacheConfiguration redisCacheConfiguration,
      final ObjectMapper objectMapper) {
    val stringListType =
        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(redisCacheConfiguration)
        .withCacheConfiguration(
            "accounts", cacheConfiguration(objectMapper, AccountResponse.class))
        .withCacheConfiguration(
            "accountList", cacheConfiguration(objectMapper, AccountListResponse.class))
        .withCacheConfiguration(
            "allowedNicknameWords", cacheConfiguration(objectMapper, stringListType))
        .withCacheConfiguration(
            "blockedNicknameWords", cacheConfiguration(objectMapper, stringListType))
        .build();
  }

  private static <T> RedisCacheConfiguration cacheConfiguration(
      final ObjectMapper objectMapper, final Class<T> type) {
    return cacheConfiguration(new JacksonJsonRedisSerializer<>(objectMapper, type));
  }

  private static RedisCacheConfiguration cacheConfiguration(
      final ObjectMapper objectMapper, final JavaType type) {
    return cacheConfiguration(new JacksonJsonRedisSerializer<>(objectMapper, type));
  }

  private static RedisCacheConfiguration cacheConfiguration(final RedisSerializer<?> serializer) {
    val valueSerializer = RedisSerializationContext.SerializationPair.fromSerializer(serializer);
    val keySerializer =
        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());

    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(DEFAULT_TTL)
        .serializeKeysWith(keySerializer)
        .serializeValuesWith(valueSerializer);
  }
}
