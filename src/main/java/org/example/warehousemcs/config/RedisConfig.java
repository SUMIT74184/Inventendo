package org.example.warehousemcs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


import java.time.Duration;

    @Configuration
    @EnableCaching
    public class RedisConfig {

        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator
                    .builder()
                    .allowIfBaseType(Object.class)
                    .build();

            objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);

            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .serializeKeysWith(
                            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                    )
                    .serializeValuesWith(
                            RedisSerializationContext.SerializationPair.fromSerializer(
                                    RedisSerializer.json()
                            )
                    );

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(config)
                    .build();
        }

        @Bean
        public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory connectionFactory){
            RedisTemplate<String,Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(serializer);
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(serializer);

            template.afterPropertiesSet();
            return template;
        }



    }





