package com.geekq.miaosha.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;

@Component
@Data
@ConfigurationProperties(prefix="redis")
public class RedisConfig extends CachingConfigurerSupport {
    private String host;
    private int port;
    private int timeout;//秒
    private String password;
    private int poolMaxTotal;
    private int poolMaxIdle;
    private int poolMaxWait;//秒
    private String master;
    private String redisNodes;


    /**
     * Custom caching key generation policy. The default generation strategy is unknown (scrambled content) custom configuration injection through Spring's dependency injection feature and this class is a configuration class that can customize configuration to a greater degree**@return
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder sb = new StringBuilder();
                sb.append(target.getClass().getName());
                sb.append(method.getName());
                for (Object obj : params) {
                    sb.append(obj.toString());
                }
                return sb.toString();
            }
        };
    }

    /**
     * Cache configuration manager*/
    @Bean
    @Override
    public CacheManager cacheManager() {
        //Creating RedisCacheWriter objects by lock writing
        RedisCacheWriter writer = RedisCacheWriter.lockingRedisCacheWriter(getConnectionFactory());
        /*
        The Value serialization of CacheManager is JdkSerializationRedisSerializer.But in fact, RedisCacheConfiguration acquiescence is used.StringRedisSerializer serializer key,JdkSerializationRedisSerializer serializer value,So the following notesCode is the default implementation, no need to write, directly annotated.*/
        // RedisSerializationContext.SerializationPair pair = RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(this.getClass().getClassLoader()));
        // RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(pair);
        //Create default cache configuration object
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        RedisCacheManager cacheManager = new RedisCacheManager(writer, config);
        return cacheManager;
    }

    /**
     * Get caching operation assistant object**@return
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        //Create Redis cache operation assistant RedisTemplate object
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(getConnectionFactory());
        //The following code replaces RedisTemplate's Value serializer with Jackson 2 Json RedisSerializer from JdkSerialization RedisSerializer //This serialization method is clear, easy to read, store less byte and fast, so it is recommended to replace it.
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setKeySerializer(new StringRedisSerializer());//RedisTemplateThe object needs to specify the Key serialization mode. If the StringRedisTemplate object is declared, it does not need it.//template.setEnableTransactionSupport(true);//Enable transaction
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Get cache connections**@return
     */
    @Bean
    public RedisConnectionFactory getConnectionFactory() {
        //standalone mode
        /* host 哨兵不需要 host port
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host); // host 哨兵不需要 host
        configuration.setPort(port); // host 哨兵不需要 port
        */
        //  configuration.setDatabase(database);
        //  configuration.setPassword(RedisPassword.of(password));
        //Sentinel model//RedisSentinelConfiguration configuration1 = new RedisSentinelConfiguration();
        //Cluster model//RedisClusterConfiguration configuration2 = new RedisClusterConfiguration();
        // 单机版
        // LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration, getPoolConfig());
        // 哨兵版 sentinel
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisSentinelConfiguration(), getPoolConfig());
        //factory.setShareNativeConnection(false);//If multiple thread operations are allowed to share the same cache connection, the default is true, and each operation opens a new connection when false
        return factory;
    }

    /**
     * Get cache connection pool**@return
     */
    @Bean
    public LettucePoolingClientConfiguration getPoolConfig() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(poolMaxTotal);
        config.setMaxWaitMillis(poolMaxWait);
        config.setMaxIdle(poolMaxIdle);
        LettucePoolingClientConfiguration pool = LettucePoolingClientConfiguration.builder()
                .poolConfig(config)
                .commandTimeout(Duration.ofMillis(timeout))
                .build();
        return pool;
    }

    /**
     * redis哨兵配置
     * @return
     */
    @Bean
    public RedisSentinelConfiguration redisSentinelConfiguration(){
        RedisSentinelConfiguration configuration = new RedisSentinelConfiguration();
        String[] host = redisNodes.split(",");
        for(String redisHost : host){
            String[] item = redisHost.split(":");
            String ip = item[0];
            String port = item[1];
            configuration.addSentinel(new RedisNode(ip, Integer.parseInt(port)));
        }
        configuration.setMaster(master);
        return configuration;
    }
}