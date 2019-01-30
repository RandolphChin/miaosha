package com.geekq.miaosha.redis.redismanager;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisManager {

    private static JedisPool jedisPool;

    static {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxWaitMillis(20);
        jedisPoolConfig.setMaxIdle(10);
        // 10.1.225.47为部署 redis 服务器的 ip
        jedisPool = new JedisPool(jedisPoolConfig,"10.1.225.47");
    }

    public static Jedis getJedis() throws Exception{
        if(null!=jedisPool){
            return jedisPool.getResource();
        }
        throw new Exception("Jedispool was not init !!!");
    }


}
